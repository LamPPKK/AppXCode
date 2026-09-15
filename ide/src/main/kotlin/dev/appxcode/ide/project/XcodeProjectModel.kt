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
enum class XcodeConfigurationOwnerKind { PROJECT, TARGET, UNKNOWN }
data class XcodeBuildConfiguration(
    val name: String,
    val buildSettings: Map<String, String>,
    val id: String = "",
    val ownerId: String? = null,
    val ownerName: String? = null,
    val ownerKind: XcodeConfigurationOwnerKind = XcodeConfigurationOwnerKind.UNKNOWN,
)

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
        return pbxObjects(text).filter { TARGET_ISA.containsMatchIn(it.body) }.mapNotNull { (_, block) ->
            val name = pbxField(block, "name") ?: return@mapNotNull null
            val product = pbxField(block, "productName")
            val type = pbxField(block, "productType")
            XcodeTarget(name, product, type)
        }.distinctBy(XcodeTarget::name).sortedBy { it.name.lowercase() }.toList()
    }

    fun readBuildConfigurations(project: Path): List<XcodeBuildConfiguration> {
        val pbx = if (project.fileName?.toString()?.endsWith(".xcodeproj") == true) project.resolve("project.pbxproj") else project
        if (!Files.isRegularFile(pbx)) return emptyList()
        val text = runCatching { Files.readString(pbx) }.getOrNull() ?: return emptyList()
        val objects = pbxObjects(text).toList()
        val configurationToList = objects.filter { CONFIGURATION_LIST_ISA.containsMatchIn(it.body) }
            .flatMap { list -> PBX_ID.findAll(pbxField(list.body, "buildConfigurations").orEmpty()).map { it.value to list.id } }
            .toMap()
        val owners = objects.mapNotNull { objectValue ->
            val kind = when {
                TARGET_ISA.containsMatchIn(objectValue.body) -> XcodeConfigurationOwnerKind.TARGET
                PROJECT_ISA.containsMatchIn(objectValue.body) -> XcodeConfigurationOwnerKind.PROJECT
                else -> return@mapNotNull null
            }
            val listId = PBX_ID.find(pbxField(objectValue.body, "buildConfigurationList").orEmpty())?.value ?: return@mapNotNull null
            listId to ConfigurationOwner(objectValue.id, pbxField(objectValue.body, "name"), kind)
        }.toMap()
        return objects.filter { CONFIGURATION_ISA.containsMatchIn(it.body) }.mapNotNull { objectValue ->
            val block = objectValue.body
            val name = pbxField(block, "name") ?: return@mapNotNull null
            val owner = configurationToList[objectValue.id]?.let(owners::get)
            XcodeBuildConfiguration(name, readBuildSettings(block), objectValue.id, owner?.id, owner?.name, owner?.kind ?: XcodeConfigurationOwnerKind.UNKNOWN)
        }.distinctBy(XcodeBuildConfiguration::id)
            .sortedWith(compareBy<XcodeBuildConfiguration>({ it.ownerKind.name }, { it.ownerName.orEmpty() }, { it.name.lowercase() }, { it.id }))
            .toList()
    }

    private fun readBuildSettings(block: String): Map<String, String> {
        val assignment = Regex("(?m)^\\s*buildSettings\\s*=\\s*\\{").find(block) ?: return emptyMap()
        val open = assignment.range.last
        val close = matchingBrace(block, open)
        if (close <= open) return emptyMap()
        val settings = block.substring(open + 1, close)
        return parseAssignments(settings).toSortedMap()
    }

    private fun parseAssignments(text: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var start = 0
        var equals = -1
        var depth = 0
        var inString = false
        var escaped = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (inString) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') inString = false
            } else when (char) {
                '"' -> inString = true
                '(', '[', '{' -> depth++
                ')', ']', '}' -> if (depth > 0) depth--
                '=' -> if (depth == 0 && equals < 0) equals = index
                ';' -> if (depth == 0 && equals >= 0) {
                    val key = unquote(text.substring(start, equals).trim())
                    val value = unquote(text.substring(equals + 1, index).trim())
                    if (key.isNotBlank()) result[key] = value
                    start = index + 1
                    equals = -1
                }
            }
            index++
        }
        return result
    }

    private data class PbxObject(val id: String, val body: String)
    private data class ConfigurationOwner(val id: String, val name: String?, val kind: XcodeConfigurationOwnerKind)

    private fun pbxObjects(text: String): Sequence<PbxObject> = sequence {
        val source = stripPbxComments(text)
        for (header in PBX_OBJECT_HEADER.findAll(source)) {
            val open = header.range.last
            val close = matchingBrace(source, open)
            if (close > open) yield(PbxObject(header.groupValues[1], source.substring(open + 1, close)))
        }
    }

    private fun stripPbxComments(text: String): String {
        val result = StringBuilder(text.length)
        var inString = false
        var escaped = false
        var lineComment = false
        var blockComment = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            val next = text.getOrNull(index + 1)
            when {
                lineComment -> {
                    if (char == '\n' || char == '\r') { lineComment = false; result.append(char) } else result.append(' ')
                }
                blockComment -> {
                    if (char == '*' && next == '/') { result.append("  "); index++; blockComment = false }
                    else result.append(if (char == '\n' || char == '\r') char else ' ')
                }
                inString -> {
                    result.append(char)
                    if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') inString = false
                }
                char == '"' -> { inString = true; result.append(char) }
                char == '/' && next == '/' -> { result.append("  "); index++; lineComment = true }
                char == '/' && next == '*' -> { result.append("  "); index++; blockComment = true }
                else -> result.append(char)
            }
            index++
        }
        return result.toString()
    }

    private fun matchingBrace(text: String, open: Int): Int {
        if (open < 0) return -1
        var depth = 0
        var inString = false
        var escaped = false
        var index = open
        while (index < text.length) {
            val char = text[index]
            if (inString) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') inString = false
            } else when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> if (--depth == 0) return index
            }
            index++
        }
        return -1
    }

    private fun pbxField(block: String, field: String): String? {
        val value = Regex("(?m)^\\s*${Regex.escape(field)}\\s*=\\s*(\\\"(?:\\\\.|[^\\\"])*\\\"|[^;]+);")
            .find(block)?.groupValues?.get(1)?.trim() ?: return null
        return unquote(value)
    }

    private fun unquote(value: String): String = if (value.startsWith('"') && value.endsWith('"') && value.length >= 2)
        value.substring(1, value.length - 1).replace("\\\"", "\"").replace("\\\\", "\\") else value

    private val PBX_OBJECT_HEADER = Regex("(?m)^\\s*([A-Fa-f0-9]{24})\\s*=\\s*\\{")
    private val PBX_ID = Regex("[A-Fa-f0-9]{24}")
    private val TARGET_ISA = Regex("(?m)^\\s*isa\\s*=\\s*PBXNativeTarget\\s*;")
    private val CONFIGURATION_ISA = Regex("(?m)^\\s*isa\\s*=\\s*XCBuildConfiguration\\s*;")
    private val CONFIGURATION_LIST_ISA = Regex("(?m)^\\s*isa\\s*=\\s*XCConfigurationList\\s*;")
    private val PROJECT_ISA = Regex("(?m)^\\s*isa\\s*=\\s*PBXProject\\s*;")

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
