package dev.appxcode.ide.project

import java.nio.file.Files
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

enum class XcodeContainerKind { PROJECT, WORKSPACE }

data class XcodeContainer(
    val path: Path,
    val kind: XcodeContainerKind,
    val schemes: List<String> = emptyList(),
) {
    val displayName: String get() = path.fileName?.toString()?.substringBeforeLast('.') ?: path.toString()
}

data class XcodeScheme(val name: String, val buildables: List<String>, val testables: List<String>)
data class XcodeTarget(val name: String, val productName: String?, val productType: String?)

/** Discovers Xcode containers without converting or rewriting their native files. */
object XcodeProjectModel {
    fun readSchemes(container: XcodeContainer): List<XcodeScheme> {
        if (!Files.isDirectory(container.path)) return emptyList()
        val roots = listOf(container.path.resolve("xcshareddata/xcschemes"), container.path.resolve("xcuserdata"))
        return roots.filter(Files::isDirectory).flatMap { root -> runCatching {
            Files.walk(root).use { files ->
                files.iterator().asSequence()
                    .filter { it.fileName.toString().endsWith(".xcscheme") }
                    .mapNotNull(::readScheme)
                    .toList()
            }
        }.getOrDefault(emptyList())
        }.distinctBy(XcodeScheme::name).sortedBy { it.name.lowercase() }
    }

    fun readTargets(project: Path): List<XcodeTarget> {
        val pbx = if (project.fileName?.toString()?.endsWith(".xcodeproj") == true) project.resolve("project.pbxproj") else project
        if (!Files.isRegularFile(pbx)) return emptyList()
        val text = runCatching { Files.readString(pbx) }.getOrNull() ?: return emptyList()
        val blocks = text.split("PBXNativeTarget = {").drop(1)
        return blocks.mapNotNull { block ->
            val name = Regex("name = ([^;]+);").find(block)?.groupValues?.get(1)?.trim() ?: return@mapNotNull null
            val product = Regex("productName = ([^;]+);").find(block)?.groupValues?.get(1)?.trim()
            val type = Regex("productType = ([^;]+);").find(block)?.groupValues?.get(1)?.trim()
            XcodeTarget(name, product, type)
        }.distinctBy(XcodeTarget::name).sortedBy { it.name.lowercase() }
    }

    fun readScheme(path: Path): XcodeScheme? {
        if (!Files.isRegularFile(path) || !path.fileName.toString().endsWith(".xcscheme")) return null
        val document = runCatching {
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
                isXIncludeAware = false
                isExpandEntityReferences = false
            }.newDocumentBuilder().parse(path.toFile())
        }.getOrNull() ?: return null
        if (document.documentElement?.tagName != "Scheme") return null
        val buildables = descendantReferences(document.documentElement, "BuildActionEntry", "BuildableName")
        val testables = descendantReferences(document.documentElement, "TestableReference", "BlueprintName")
        return XcodeScheme(path.fileName.toString().removeSuffix(".xcscheme"), buildables, testables)
    }

    private fun descendantReferences(root: Element, parentTag: String, attribute: String): List<String> = buildList {
        val parents = root.getElementsByTagName(parentTag)
        for (index in 0 until parents.length) {
            val parent = parents.item(index) as? Element ?: continue
            val references = parent.getElementsByTagName("BuildableReference")
            for (referenceIndex in 0 until references.length) {
                val value = (references.item(referenceIndex) as? Element)?.getAttribute(attribute).orEmpty()
                if (value.isNotBlank()) add(value)
            }
        }
    }.distinct()
    fun discover(root: Path): List<XcodeContainer> {
        if (!Files.isDirectory(root)) return emptyList()
        val containers = mutableListOf<XcodeContainer>()
        runCatching {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir != root && dir.fileName.toString() in IGNORED_DIRECTORIES) return FileVisitResult.SKIP_SUBTREE
                    val name = dir.fileName?.toString().orEmpty()
                    val kind = when {
                        name.endsWith(".xcworkspace") -> XcodeContainerKind.WORKSPACE
                        name.endsWith(".xcodeproj") -> XcodeContainerKind.PROJECT
                        else -> null
                    }
                    if (kind != null) {
                        containers.add(container(dir, kind))
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult = FileVisitResult.CONTINUE
            })
        }
        return containers.distinctBy { it.path.toAbsolutePath().normalize() }
            .sortedWith(compareBy<XcodeContainer>({ it.displayName.lowercase() }, { it.path.toString() }))
    }

    private fun container(path: Path, kind: XcodeContainerKind): XcodeContainer {
        val shared = path.resolve("xcshareddata/xcschemes")
        val user = path.resolve("xcuserdata")
        val schemeRoots = listOf(shared, user).filter(Files::isDirectory)
        val schemes = schemeRoots.flatMap { root -> runCatching {
            Files.walk(root).use { files ->
                files.iterator().asSequence()
                    .filter { it.fileName.toString().endsWith(".xcscheme") }
                    .map { it.fileName.toString().removeSuffix(".xcscheme") }
                    .toList()
            }
        }.getOrDefault(emptyList())
        }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        return XcodeContainer(path, kind, schemes)
    }

    private val IGNORED_DIRECTORIES = setOf(".git", ".gradle", ".build", "build", "Pods", "DerivedData", ".dart_tool")
}
