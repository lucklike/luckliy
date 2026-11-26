package com.luckyframework.idea.spel.environment

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * 项目中的 Spring 配置属性定义。
 *
 * [keyOffset] 指向键名起点、[valueOffset] 指向值起点，均相对配置文件；
 * [profile] 是文件名里的 profile 后缀（`application-dev.yml` 为 `dev`），
 * 默认文件为 null；[rank] 是配置文档的阶段优先级，null 表示该文件没有被
 * 激活的 profile 加载；[order] 是文档加载顺序，`spring.config.import` 导入的
 * 文件排在导入者之后，同一键取 [order] 最大（最后加载）的定义生效。
 */
data class SpringPropertyDefinition(
    val key: String,
    val value: String,
    val file: VirtualFile,
    val keyOffset: Int,
    val valueOffset: Int,
    val profile: String?,
    val rank: Int?,
    val order: Int = 0
) {

    val fileName: String get() = file.name

    /** 定义来自 YAML 文件；重命名时 YAML 插件只传递叶子键名。 */
    val isYaml: Boolean
        get() = file.extension?.lowercase() == "yml" || file.extension?.lowercase() == "yaml"

    val profileLabel: String? get() = profile?.takeIf { it.isNotBlank() && it != "default" }
}

/** 一个模块的配置快照：[definitions] 保留全部定义，[effective] 是实际生效的那一份。 */
class SpringPropertySnapshot(
    val definitions: Map<String, List<SpringPropertyDefinition>>,
    val effective: Map<String, SpringPropertyDefinition>,
    val activeProfiles: List<String>
) {

    fun keys(): List<String> = effective.keys.sorted()

    fun definitionsOf(key: String): List<SpringPropertyDefinition> =
        definitions[key].orEmpty().sortedWith(
            compareByDescending<SpringPropertyDefinition> { it.order }
                .thenByDescending { it.rank ?: -1 }
                .thenBy { it.file.path }
        )
}

/**
 * 扫描模块（含依赖模块）中的 Spring Boot 配置文件。
 *
 * 索引 `application(-profile)?` 与 `bootstrap(-profile)?` 的 properties/yaml
 * 文件，以及它们通过 `spring.config.import` 递归导入的配置文件；不解析
 * 依赖 jar 内的配置。内容按纯文本扁平化：
 * - properties 支持 `key=value` / `key:value` / `key value`；
 * - YAML 支持嵌套映射（`server.port`）与带引号的键值，列表与锚点不求值；
 * - 激活 profile 由 `spring.profiles.active` 决定，profile 文件优先级高于默认文件；
 * - 同一位置 properties 高于 yaml（与 Spring Boot 的加载顺序一致）；
 * - `spring.config.import` 支持 `optional:` 前缀、`classpath:` / `file:` /
 *   无前缀（按 classpath 处理）三类位置，YAML 列表与 properties 逗号分隔；
 *   导入文档在导入者之后加载，因此同一键优先取导入文件里的定义。
 */
class SpringPropertyIndex {

    fun snapshot(module: Module): SpringPropertySnapshot {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val project = module.project
        val files = ArrayList<VirtualFile>()
        for (extension in INDEXED_EXTENSIONS) {
            files += FilenameIndex.getAllFilesByExt(project, extension, scope)
        }
        val baseFiles = files.distinct()
            .filter { CONFIG_FILE_NAME.matches(it.name) }
            .map { BaseDocument(it, profileOf(it.name), stageOf(it)) }
            .sortedWith(compareBy({ it.stage }, { it.file.path }))
        val parsed = HashMap<VirtualFile, ParsedDocument>()
        fun parsed(file: VirtualFile): ParsedDocument =
            parsed.getOrPut(file) { parseFile(project, file) }

        val activeProfiles = activeProfilesOf(baseFiles.flatMap { parsed(it.file).definitions })
        val definitions = ArrayList<SpringPropertyDefinition>()
        val visited = HashSet<VirtualFile>()
        var order = 0

        fun load(file: VirtualFile, profile: String?, rank: Int?, depth: Int) {
            if (depth > MAX_IMPORT_DEPTH || !visited.add(file)) {
                return
            }
            val document = parsed(file)
            for (raw in document.definitions) {
                definitions += SpringPropertyDefinition(
                    key = raw.key,
                    value = raw.value,
                    file = raw.file,
                    keyOffset = raw.keyOffset,
                    valueOffset = raw.valueOffset,
                    profile = profile,
                    rank = rank,
                    order = order
                )
            }
            order++
            // 未激活的 profile 文档不会被 Spring 加载，它声明的 import 也不生效
            if (rank == null) {
                return
            }
            for (location in document.imports) {
                for (imported in resolveImports(project, module, scope, location)) {
                    load(imported, null, rank, depth + 1)
                }
            }
        }

        for (document in baseFiles) {
            load(document.file, document.profile, rankOf(document, activeProfiles), 0)
        }

        val grouped = definitions.groupBy { it.key }
        val effective = grouped.mapNotNull { (key, candidates) ->
            candidates.filter { it.rank != null }
                .maxByOrNull { it.order }
                ?.let { key to it }
        }.toMap()
        return SpringPropertySnapshot(grouped, effective, activeProfiles)
    }

    /** 基础的 application/bootstrap 文档：profile 由文件名决定。 */
    private class BaseDocument(
        val file: VirtualFile,
        val profile: String?,
        val stage: Int
    )

    private fun activeProfilesOf(raw: List<RawDefinition>): List<String> {
        val candidates = raw.filter { it.key == SPRING_PROFILES_ACTIVE && it.profile == null }
        val value = candidates
            .sortedWith(compareByDescending<RawDefinition> { it.file.name.startsWith("application") }
                .thenByDescending { it.file.extension == "properties" })
            .firstOrNull()
            ?.value
            ?: return emptyList()
        return value.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "default" }
            .distinct()
    }

    /**
     * 文件阶段。application 高于 bootstrap；激活的 profile 文件高于默认文件；
     * 同一档位 properties 高于 yaml（yaml 先加载，properties 后加载）。
     */
    private fun stageOf(file: VirtualFile): Int {
        val bootstrap = file.name.startsWith("bootstrap", ignoreCase = true)
        val profile = profileOf(file.name)
        val base = when {
            profile != null && profile != "default" -> if (bootstrap) 80 else 100
            else -> if (bootstrap) 70 else 90
        }
        return base + if (file.extension?.equals("properties", ignoreCase = true) == true) 1 else 0
    }

    /** 未激活 profile 的文件不参与生效值，但仍保留定义供展示与跳转。 */
    private fun rankOf(document: BaseDocument, activeProfiles: List<String>): Int? {
        val profile = document.profile
        val matchesProfile = profile == null ||
            profile == "default" ||
            profile.split(',').map { it.trim() }.any { it in activeProfiles }
        return document.stage.takeIf { matchesProfile }
    }

    /**
     * 文件当前文本：优先取 PSI 内容。
     *
     * 重命名配置键后文件通常尚未保存，VFS 里仍是旧内容；按 VFS 文本解析会
     * 得到重命名前的键与偏移，导致 `${key}` 跳不到配置、误报未定义。PSI 内容
     * 始终与 [SpringPropertyDefinition.keyOffset] 的定位基准一致。
     */
    private fun textOf(project: Project, file: VirtualFile): String? {
        PsiManager.getInstance(project).findFile(file)?.let { return it.text }
        return runCatching { VfsUtilCore.loadText(file).toString() }.getOrNull()
    }

    private fun parseFile(project: Project, file: VirtualFile): ParsedDocument {
        val text = textOf(project, file) ?: return ParsedDocument.EMPTY
        val profile = profileOf(file.name)
        return when (file.extension?.lowercase()) {
            "properties" -> parseProperties(text, file, profile)
            "yml", "yaml" -> parseYaml(text, file, profile)
            else -> ParsedDocument.EMPTY
        }
    }

    private fun profileOf(fileName: String): String? {
        val base = fileName.substringBeforeLast('.')
        val suffix = base.substringAfter('-', "")
        return suffix.takeIf { it.isNotBlank() }
    }

    private fun parseProperties(
        text: String,
        file: VirtualFile,
        profile: String?
    ): ParsedDocument {
        val result = ArrayList<RawDefinition>()
        val imports = ArrayList<String>()
        var offset = 0
        for (line in text.split('\n')) {
            val lineStart = offset
            offset += line.length + 1
            val trimmedStart = line.indexOfFirst { !it.isWhitespace() }
            if (trimmedStart < 0) {
                continue
            }
            val first = line[trimmedStart]
            if (first == '#' || first == '!') {
                continue
            }
            val separator = propertySeparator(line, trimmedStart)
            if (separator < 0) {
                val key = line.substring(trimmedStart).trim()
                if (key.isNotEmpty()) {
                    result += raw(key, "", file, lineStart + trimmedStart, lineStart + line.length, profile)
                }
                continue
            }
            val key = line.substring(trimmedStart, separator).trim()
            if (key.isEmpty()) {
                continue
            }
            var valueStart = separator + 1
            while (valueStart < line.length && line[valueStart].isWhitespace()) {
                valueStart++
            }
            if (valueStart < line.length && (line[valueStart] == '=' || line[valueStart] == ':')) {
                valueStart++
                while (valueStart < line.length && line[valueStart].isWhitespace()) {
                    valueStart++
                }
            }
            val value = line.substring(valueStart).trim()
            if (isImportKey(key)) {
                imports += splitLocations(value)
            }
            result += raw(
                key = key,
                value = value,
                file = file,
                keyOffset = lineStart + trimmedStart,
                valueOffset = lineStart + valueStart,
                profile = profile
            )
        }
        return ParsedDocument(result, imports)
    }

    private fun propertySeparator(line: String, start: Int): Int {
        var index = start
        while (index < line.length) {
            val current = line[index]
            if (current == '\\') {
                index += 2
                continue
            }
            if (current == '=' || current == ':' || current.isWhitespace()) {
                return index
            }
            index++
        }
        return -1
    }

    /**
     * 把 YAML 映射扁平化为 `a.b.c` 键。
     *
     * 只处理缩进映射；列表元素、锚点/别名和块标量不参与键值解析，
     * 避免把 `- name: x` 误当成顶层键或给出错误的值预览。
     * `spring.config.import` 例外：标量形式记入 `imports`；列表形式下父键
     * 入栈后 `- location` 行也作为导入位置收集。
     */
    private fun parseYaml(
        text: String,
        file: VirtualFile,
        profile: String?
    ): ParsedDocument {
        val result = ArrayList<RawDefinition>()
        val imports = ArrayList<String>()
        val lines = text.split('\n')
        val stack = ArrayDeque<Pair<Int, String>>()
        var offset = 0
        for (line in lines) {
            val lineStart = offset
            offset += line.length + 1
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed == "---" || trimmed == "...") {
                continue
            }
            val indent = line.indexOfFirst { !it.isWhitespace() }
            if (indent < 0) {
                continue
            }
            if (trimmed.startsWith("-")) {
                if (stackPathOf(stack) == SPRING_CONFIG_IMPORT) {
                    val item = unquote(stripYamlComment(trimmed.substring(1).trim()).trim())
                    imports += splitLocations(item)
                }
                continue
            }
            while (stack.isNotEmpty() && stack.last().first >= indent) {
                stack.removeLast()
            }
            val content = line.substring(indent)
            val colon = yamlColon(content)
            if (colon < 0) {
                continue
            }
            val rawKey = unquote(content.substring(0, colon).trim())
            if (rawKey.isEmpty()) {
                continue
            }
            val path = (stack.map { it.second } + rawKey).joinToString(".")
            val rawValue = content.substring(colon + 1)
            val value = stripYamlComment(rawValue).trim()
            if (value.isEmpty() || value.startsWith("&") || value.startsWith("*") ||
                value == "|" || value == ">" || value == "|-" || value == ">-"
            ) {
                // 映射/列表/块标量的父键，继续向下解析；本身不产生可预览值
                stack.addLast(indent to rawKey)
                continue
            }
            val rawText = unquote(value)
            if (path == SPRING_CONFIG_IMPORT) {
                imports += splitLocations(rawText)
            }
            val valueOffsetInLine = indent + colon + 1 + rawValue.indexOfFirst { !it.isWhitespace() }
                .coerceAtLeast(0)
            result += raw(
                key = path,
                value = rawText,
                file = file,
                keyOffset = lineStart + indent,
                valueOffset = (lineStart + valueOffsetInLine).coerceAtMost(lineStart + line.length),
                profile = profile
            )
        }
        return ParsedDocument(result, imports)
    }

    private fun stackPathOf(stack: ArrayDeque<Pair<Int, String>>): String =
        stack.joinToString(".") { it.second }

    /** 找到 YAML 键值分隔的 `:`：跳过引号内的冒号，并要求后面是空白或行尾。 */
    private fun yamlColon(content: String): Int {
        var quote: Char? = null
        var index = 0
        while (index < content.length) {
            val current = content[index]
            if (quote != null) {
                if (current == '\\' && quote == '"') {
                    index += 2
                    continue
                }
                if (current == quote) {
                    quote = null
                }
                index++
                continue
            }
            if (current == '\'' || current == '"') {
                quote = current
                index++
                continue
            }
            if (current == ':' &&
                (index + 1 >= content.length || content[index + 1].isWhitespace())
            ) {
                return index
            }
            index++
        }
        return -1
    }

    private fun stripYamlComment(value: String): String {
        var quote: Char? = null
        var index = 0
        while (index < value.length) {
            val current = value[index]
            if (quote != null) {
                if (current == '\\' && quote == '"') {
                    index += 2
                    continue
                }
                if (current == quote) {
                    quote = null
                }
                index++
                continue
            }
            if (current == '\'' || current == '"') {
                quote = current
                index++
                continue
            }
            if (current == '#' && (index == 0 || value[index - 1].isWhitespace())) {
                return value.substring(0, index)
            }
            index++
        }
        return value
    }

    private fun unquote(value: String): String {
        if (value.length < 2) {
            return value
        }
        val first = value.first()
        val last = value.last()
        if (first == '\'' && last == '\'') {
            return value.substring(1, value.length - 1).replace("''", "'")
        }
        if (first == '"' && last == '"') {
            return value.substring(1, value.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
        }
        return value
    }

    /** properties 的 `spring.config.import` 也可能是 `import[0]` 列表形式。 */
    private fun isImportKey(key: String): Boolean =
        key == SPRING_CONFIG_IMPORT || key.startsWith("$SPRING_CONFIG_IMPORT[")

    /** `spring.config.import` 可写多个位置，逗号分隔；完整值也可能是 YAML flow 列表。 */
    private fun splitLocations(value: String): List<String> =
        value.trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * 把一个 import 位置解析成项目内的配置文件。
     *
     * 支持 `optional:` 前缀（只影响“缺失是否报错”，静态索引一律跳过缺失位置）、
     * `classpath:` / 无前缀（按 classpath 根定位）与 `file:`（绝对路径或相对
     * 项目根/模块内容根）；`configtree:`、`http(s):` 等非文件位置与含占位符的
     * 位置不参与索引。
     */
    private fun resolveImports(
        project: Project,
        module: Module,
        scope: GlobalSearchScope,
        rawLocation: String
    ): List<VirtualFile> {
        var location = rawLocation.trim()
        while (location.startsWith(OPTIONAL_PREFIX, ignoreCase = true)) {
            location = location.substring(OPTIONAL_PREFIX.length).trim()
        }
        if (location.isEmpty() || location.contains("\${")) {
            return emptyList()
        }
        return when {
            location.startsWith(FILE_PREFIX, ignoreCase = true) ->
                resolveFileLocation(project, module, location.substring(FILE_PREFIX.length))

            location.startsWith(CLASSPATH_PREFIX, ignoreCase = true) ->
                resolveClasspathLocation(project, scope, location.substring(CLASSPATH_PREFIX.length))

            hasScheme(location) -> emptyList()
            else -> resolveClasspathLocation(project, scope, location)
        }
    }

    /** 第一个冒号前是纯字母时才当作 scheme，Windows 盘符（`C:`）不算。 */
    private fun hasScheme(location: String): Boolean {
        val colon = location.indexOf(':')
        if (colon <= 1) {
            return false
        }
        return location.substring(0, colon).all { it.isLetter() }
    }

    /** classpath 位置：在模块（含依赖模块）资源目录中按相对路径匹配同名文件。 */
    private fun resolveClasspathLocation(
        project: Project,
        scope: GlobalSearchScope,
        rawPath: String
    ): List<VirtualFile> {
        val path = rawPath.trim().trimStart('/')
        if (path.isEmpty() || path.endsWith('/')) {
            return emptyList()
        }
        val name = path.substringAfterLast('/')
        if (name.isEmpty()) {
            return emptyList()
        }
        return FilenameIndex.getVirtualFilesByName(name, scope)
            .filter { matchesClasspathPath(project, it, path) }
            .sortedBy { it.path }
    }

    /**
     * 文件的 classpath 相对路径是否与导入位置一致。
     *
     * 真实项目中资源根（`src/main/resources`）已注册为 source root；测试或
     * Gradle 临时项目可能只有内容根，因此额外比内容根相对路径与常见资源
     * 目录前缀，最后用绝对路径后缀兜底。
     */
    private fun matchesClasspathPath(
        project: Project,
        file: VirtualFile,
        path: String
    ): Boolean {
        if (file.isDirectory) {
            return false
        }
        val index = ProjectFileIndex.getInstance(project)
        index.getSourceRootForFile(file)?.let { root ->
            if (VfsUtilCore.getRelativePath(file, root, '/') == path) {
                return true
            }
        }
        index.getContentRootForFile(file)?.let { root ->
            val relative = VfsUtilCore.getRelativePath(file, root, '/') ?: return@let
            if (relative == path || relative.endsWith("/src/main/resources/$path") ||
                relative.endsWith("/src/test/resources/$path") ||
                relative == "src/main/resources/$path" || relative == "src/test/resources/$path"
            ) {
                return true
            }
        }
        return file.path.endsWith("/$path")
    }

    /** `file:` 位置：绝对路径直接查找，相对路径基于项目根与模块内容根解析。 */
    private fun resolveFileLocation(
        project: Project,
        module: Module,
        rawPath: String
    ): List<VirtualFile> {
        var path = rawPath.trim()
        if (path.isEmpty()) {
            return emptyList()
        }
        // file:///etc/x 是 Unix 绝对路径；file:///C:/x 在 Windows 下去掉前导斜杠
        if (path.startsWith("///")) {
            val rest = path.substring(3)
            path = if (WINDOWS_ABSOLUTE_PATH.matches(rest)) rest else "/$rest"
        } else if (path.startsWith("//")) {
            path = path.substring(2)
        }
        val local = LocalFileSystem.getInstance()
        if (isAbsolutePath(path)) {
            return listOfNotNull(local.findFileByPath(path.replace('\\', '/')))
                .filterNot { it.isDirectory }
        }
        val bases = LinkedHashSet<VirtualFile>()
        project.basePath?.let { local.findFileByPath(it) }?.let { bases += it }
        bases += ModuleRootManager.getInstance(module).contentRoots
        return bases.mapNotNull { findRelative(it, path) }
            .filterNot { it.isDirectory }
            .distinct()
    }

    private fun findRelative(base: VirtualFile, path: String): VirtualFile? {
        var current: VirtualFile? = base
        for (segment in path.replace('\\', '/').split('/')) {
            current = when {
                current == null -> return null
                segment.isEmpty() || segment == "." -> current
                segment == ".." -> current.parent
                else -> current.findChild(segment)
            }
        }
        return current
    }

    private fun isAbsolutePath(path: String): Boolean =
        path.startsWith("/") || path.startsWith("\\\\") || WINDOWS_ABSOLUTE_PATH.matches(path)

    private fun raw(
        key: String,
        value: String,
        file: VirtualFile,
        keyOffset: Int,
        valueOffset: Int,
        profile: String?
    ): RawDefinition = RawDefinition(key, value, file, keyOffset, valueOffset, profile)

    /** 一个配置文件解析后的扁平化定义与它声明的 `spring.config.import` 位置。 */
    private class ParsedDocument(
        val definitions: List<RawDefinition>,
        val imports: List<String>
    ) {
        companion object {
            val EMPTY = ParsedDocument(emptyList(), emptyList())
        }
    }

    private data class RawDefinition(
        val key: String,
        val value: String,
        val file: VirtualFile,
        val keyOffset: Int,
        val valueOffset: Int,
        val profile: String?
    )

    private companion object {
        const val SPRING_PROFILES_ACTIVE = "spring.profiles.active"

        const val SPRING_CONFIG_IMPORT = "spring.config.import"

        const val OPTIONAL_PREFIX = "optional:"

        const val FILE_PREFIX = "file:"

        const val CLASSPATH_PREFIX = "classpath:"

        /** Spring Boot 自身允许 32 层导入，这里同步限制防止递归失控。 */
        const val MAX_IMPORT_DEPTH = 32

        val WINDOWS_ABSOLUTE_PATH = Regex("^[A-Za-z]:[\\\\/].*")

        val INDEXED_EXTENSIONS = listOf("properties", "yml", "yaml")

        val CONFIG_FILE_NAME = Regex(
            "(?i)^(application|bootstrap)(-[^.]+)?\\.(properties|yml|yaml)$"
        )
    }
}
