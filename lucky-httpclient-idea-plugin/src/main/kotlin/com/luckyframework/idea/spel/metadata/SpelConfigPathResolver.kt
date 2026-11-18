package com.luckyframework.idea.spel.metadata

/**
 * 一条 YAML 键路径在配置模型中的落点。
 *
 * [field] 是最内层命中字段；[remaining] 是字段之后仍未消耗的路径段
 * （Map 的数据键、结构写错的多余层级等），调用方据此判断表达式属于哪个字段。
 * [placeholders] 记录 `{configId}`/`{apiId}` 这类占位符匹配到的实际键。
 */
data class SpelConfigPathMatch(
    val model: SpelConfigModel,
    val field: SpelConfigField,
    val placeholders: Map<String, String> = emptyMap(),
    val remaining: List<String> = emptyList()
)

/**
 * 把键路径映射到 `@SpelConfig` 配置模型的字段。
 *
 * 匹配规则：
 *
 * - 顶层入口是模型的 `prefix` 与 `aliases`，按 `.` 拆段逐段比较；
 * - `{xxx}` 段匹配任意一个键并记录占位符值，同名占位符必须一致；
 * - 前缀匹配后逐段下探字段：`STRUCT` 字段用 [SpelConfigField.valueType]
 *   找到嵌套模型继续；Map 字段跳过一段实例键（如 `method-configs.<apiId>`）；
 * - 字段之后不再要求匹配，多余的段进入 [SpelConfigPathMatch.remaining]。
 *
 * `prefix = ""` 的嵌套模型没有独立入口，只能经父字段的 key 进入；
 * 它们的 `aliases` 是父字段 key，不参与顶层匹配。
 */
class SpelConfigPathResolver(private val models: List<SpelConfigModel>) {

    fun resolve(path: List<String>): SpelConfigPathMatch? {
        if (path.isEmpty()) {
            return null
        }
        for (model in models) {
            if (model.config.nested) {
                continue
            }
            for (template in listOf(model.config.prefix) + model.config.aliases) {
                val segments = segmentsOf(template)
                if (segments.isEmpty() || segments.size >= path.size) {
                    continue
                }
                val placeholders = matchTemplate(segments, path) ?: continue
                fieldAt(model, segments.size, path)?.let { return it.copy(placeholders = placeholders) }
            }
        }
        return null
    }

    private fun fieldAt(start: SpelConfigModel, startIndex: Int, path: List<String>): SpelConfigPathMatch? {
        var model = start
        var index = startIndex
        var last: SpelConfigField? = null
        while (index < path.size) {
            val field = model.field(path[index]) ?: break
            last = field
            index++
            val nested = nestedModelOf(field) ?: break
            if (index >= path.size) {
                break
            }
            if (field.map) {
                // Map 的键是实例数据（apiId 等），不是模型字段
                index++
            }
            model = nested
        }
        val field = last ?: return null
        return SpelConfigPathMatch(model, field, remaining = path.drop(index))
    }

    /** 字段值类型对应的嵌套配置模型；不是配置模型时返回 null。 */
    private fun nestedModelOf(field: SpelConfigField): SpelConfigModel? {
        val type = field.valueType ?: return null
        return models.firstOrNull { it.fqName == type }
    }

    private fun matchTemplate(segments: List<String>, path: List<String>): Map<String, String>? {
        val placeholders = LinkedHashMap<String, String>()
        for ((index, segment) in segments.withIndex()) {
            if (index >= path.size) {
                return null
            }
            val name = placeholderName(segment)
            if (name == null) {
                if (segment != path[index]) {
                    return null
                }
                continue
            }
            val value = path[index]
            val existing = placeholders[name]
            if (existing != null && existing != value) {
                return null
            }
            placeholders[name] = value
        }
        return placeholders
    }

    private fun placeholderName(segment: String): String? =
        if (segment.length > 2 && segment.startsWith('{') && segment.endsWith('}')) {
            segment.substring(1, segment.length - 1).takeIf { it.isNotBlank() }
        } else {
            null
        }

    private fun segmentsOf(template: String): List<String> =
        template.split('.').filter { it.isNotBlank() }
}
