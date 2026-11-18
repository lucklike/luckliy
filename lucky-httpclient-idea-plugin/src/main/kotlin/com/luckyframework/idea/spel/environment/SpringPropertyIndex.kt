package com.luckyframework.idea.spel.environment

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
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
 * 默认文件为 null；[rank] 越高越优先，null 表示该文件没有被激活的 profile 加载。
 */
data class SpringPropertyDefinition(
    val key: String,
    val value: String,
    val file: VirtualFile,
    val keyOffset: Int,
    val valueOffset: Int,
    val profile: String?,
    val rank: Int?
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
            compareByDescending<SpringPropertyDefinition> { it.rank ?: -1 }
                .thenBy { it.file.path }
        )
}

/**
 * 扫描模块（含依赖模块）中的 Spring Boot 配置文件。
 *
 * 只索引 `application(-profile)?` 与 `bootstrap(-profile)?` 的 properties/yaml
 * 文件，不解析依赖 jar 内的配置；内容按纯文本扁平化：
 * - properties 支持 `key=value` / `key:value` / `key value`；
 * - YAML 支持嵌套映射（`server.port`）与带引号的键值，列表与锚点不求值；
 * - 激活 profile 由 `spring.profiles.active` 决定，profile 文件优先级高于默认文件；
 * - 同一位置 properties 高于 yaml（与 Spring Boot 的加载顺序一致）。
 */
class SpringPropertyIndex {

    fun snapshot(module: Module): SpringPropertySnapshot {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val project = module.project
        val files = ArrayList<VirtualFile>()
        for (extension in INDEXED_EXTENSIONS) {
            files += FilenameIndex.getAllFilesByExt(project, extension, scope)
        }
        val raw = files.distinct()
            .filter { CONFIG_FILE_NAME.matches(it.name) }
            .sortedBy { it.path }
            .flatMap { parseFile(project, it) }
        val activeProfiles = activeProfilesOf(raw)
        val definitions = raw.mapNotNull { definition ->
            val rank = rankOf(definition, activeProfiles)
            SpringPropertyDefinition(
                key = definition.key,
                value = definition.value,
                file = definition.file,
                keyOffset = definition.keyOffset,
                valueOffset = definition.valueOffset,
                profile = definition.profile,
                rank = rank
            )
        }
        val grouped = definitions.groupBy { it.key }
        val effective = grouped.mapValues { (_, candidates) ->
            candidates.reduce { best, candidate -> pick(best, candidate) }
        }.filterValues { it.rank != null }
        return SpringPropertySnapshot(grouped, effective, activeProfiles)
    }

    private fun pick(
        current: SpringPropertyDefinition,
        candidate: SpringPropertyDefinition
    ): SpringPropertyDefinition {
        val currentRank = current.rank ?: -1
        val candidateRank = candidate.rank ?: -1
        return when {
            candidateRank > currentRank -> candidate
            candidateRank < currentRank -> current
            candidate.file.path < current.file.path -> candidate
            else -> current
        }
    }

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
     * 文件优先级。application 高于 bootstrap；激活的 profile 文件高于默认文件；
     * 同一档位 properties 高于 yaml。未激活 profile 的文件不参与生效值。
     */
    private fun rankOf(definition: RawDefinition, activeProfiles: List<String>): Int? {
        val profile = definition.profile
        val matchesProfile = profile == null ||
            profile == "default" ||
            profile.split(',').map { it.trim() }.any { it in activeProfiles }
        if (!matchesProfile) {
            return null
        }
        val bootstrap = definition.file.name.startsWith("bootstrap", ignoreCase = true)
        val base = when {
            profile != null && profile != "default" -> if (bootstrap) 80 else 100
            else -> if (bootstrap) 70 else 90
        }
        return base + if (definition.isProperties) 1 else 0
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

    private fun parseFile(project: Project, file: VirtualFile): List<RawDefinition> {
        val text = textOf(project, file) ?: return emptyList()
        val profile = profileOf(file.name)
        return when (file.extension?.lowercase()) {
            "properties" -> parseProperties(text, file, profile)
            "yml", "yaml" -> parseYaml(text, file, profile)
            else -> emptyList()
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
    ): List<RawDefinition> {
        val result = ArrayList<RawDefinition>()
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
                    result += raw(key, "", file, lineStart + trimmedStart, lineStart + line.length, profile, true)
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
            result += raw(
                key = key,
                value = line.substring(valueStart).trim(),
                file = file,
                keyOffset = lineStart + trimmedStart,
                valueOffset = lineStart + valueStart,
                profile = profile,
                properties = true
            )
        }
        return result
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
     */
    private fun parseYaml(
        text: String,
        file: VirtualFile,
        profile: String?
    ): List<RawDefinition> {
        val result = ArrayList<RawDefinition>()
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
            val valueOffsetInLine = indent + colon + 1 + rawValue.indexOfFirst { !it.isWhitespace() }
                .coerceAtLeast(0)
            result += raw(
                key = path,
                value = unquote(value),
                file = file,
                keyOffset = lineStart + indent,
                valueOffset = (lineStart + valueOffsetInLine).coerceAtMost(lineStart + line.length),
                profile = profile,
                properties = false
            )
        }
        return result
    }

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

    private fun raw(
        key: String,
        value: String,
        file: VirtualFile,
        keyOffset: Int,
        valueOffset: Int,
        profile: String?,
        properties: Boolean
    ): RawDefinition = RawDefinition(key, value, file, keyOffset, valueOffset, profile, properties)

    private data class RawDefinition(
        val key: String,
        val value: String,
        val file: VirtualFile,
        val keyOffset: Int,
        val valueOffset: Int,
        val profile: String?,
        val isProperties: Boolean
    )

    private companion object {
        const val SPRING_PROFILES_ACTIVE = "spring.profiles.active"

        val INDEXED_EXTENSIONS = listOf("properties", "yml", "yaml")

        val CONFIG_FILE_NAME = Regex(
            "(?i)^(application|bootstrap)(-[^.]+)?\\.(properties|yml|yaml)$"
        )
    }
}
