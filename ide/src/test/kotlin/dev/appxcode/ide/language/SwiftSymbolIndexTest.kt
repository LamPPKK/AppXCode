package dev.appxcode.ide.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SwiftSymbolIndexTest {
    @Test
    fun indexesDeclarationsCompletionAndReferencesAcrossFiles() {
        val root = Files.createTempDirectory("appxcode-swift-index")
        val model = root.resolve("Account.swift")
        val viewModel = root.resolve("AccountViewModel.swift")
        Files.writeString(
            model,
            """
            struct Account {
                let name: String
            }
            """.trimIndent(),
        )
        Files.writeString(
            viewModel,
            """
            final class AccountViewModel {
                let account: Account
                func title() -> String { account.name }
            }
            """.trimIndent(),
        )

        val index = SwiftSymbolIndex()
        index.index(listOf(model, viewModel))

        assertEquals(listOf("Account"), index.find("Account").map { it.name }.distinct())
        assertTrue(index.complete("Acc").any { it.name == "Account" })
        val references = index.references("Account")
        assertEquals(3, references.size)
        assertTrue(references.any { it.file == model && it.line == 1 })
        assertTrue(references.any { it.file == viewModel && it.line == 1 })
        assertTrue(references.any { it.file == viewModel && it.line == 2 })

        val declarations = index.declarationTargets("Account")
        assertEquals(1, declarations.size)
        assertEquals(model, declarations.single().file)
        assertEquals(1, declarations.single().line)
        assertEquals(8, declarations.single().column)
        assertEquals("struct", declarations.single().kind)

        val usages = index.usageTargets("Account")
        assertEquals(2, usages.size)
        assertTrue(usages.any { it.file == viewModel && it.line == 1 })
        assertTrue(usages.any { it.file == viewModel && it.line == 2 })
        assertEquals(3, index.usageTargets("Account", includeDeclarations = true).size)
    }

    @Test
    fun reindexDropsStaleReferences() {
        val root = Files.createTempDirectory("appxcode-swift-reindex")
        val file = root.resolve("Feature.swift")
        Files.writeString(file, "struct Legacy {}\nlet value: Legacy")
        val index = SwiftSymbolIndex()

        index.index(listOf(file))
        assertEquals(2, index.references("Legacy").size)

        Files.writeString(file, "struct Current {}")
        index.index(listOf(file))
        assertTrue(index.references("Legacy").isEmpty())
        assertEquals(1, index.references("Current").size)
    }

    @Test
    fun indexedRenameSkipsCommentsAndStrings() {
        val root = Files.createTempDirectory("appxcode-swift-rename")
        val file = root.resolve("Feature.swift")
        Files.writeString(file, "struct Account {}\nlet value: Account\n// Account\nlet text = \"Account\"\n/* Account */")
        val index = SwiftSymbolIndex()
        index.index(listOf(file))

        assertEquals(2, index.references("Account").size)
        val preview = SymbolRenameEngine.preview(index, "Account", "Profile")
        assertEquals(2, preview.edits.size)
        SymbolRenameEngine.apply(preview)
        val updated = Files.readString(file)
        assertTrue(updated.contains("struct Profile {}"))
        assertTrue(updated.contains("let value: Profile"))
        assertTrue(updated.contains("// Account"))
        assertTrue(updated.contains("\"Account\""))
        assertTrue(updated.contains("/* Account */"))
    }
}
