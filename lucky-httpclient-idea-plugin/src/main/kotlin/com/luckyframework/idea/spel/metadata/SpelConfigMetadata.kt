package com.luckyframework.idea.spel.metadata

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

/**
 * `@SpelConfig` 声明的配置模型。
 *
 * [prefix] 为 `""` 表示嵌套模型：没有独立配置前缀，只能作为父字段的值使用，
 * 实际路径由父字段的 [SpelConfigField.key] 拼接；[aliases] 记录框架允许的父字段 key。
 *
 * [naming] 是字段名到配置 key 的命名策略，只对未显式声明 `@SpelAttribute#key`
 * 的字段生效。`prefix` 与 `aliases` 里的 `{xxx}` 段是实例占位符（如 `{configId}`、
 * `{apiId}`），匹配 YAML 路径时对应任意一个键。
 */
data class SpelConfigMetadata(
    val fqName: String,
    val prefix: String,
    val aliases: List<String> = emptyList(),
    val naming: String = NAMING_AS_IS,
    val scope: String = SCOPE_BOTH,
    val description: String = ""
) {

    /** 嵌套模型：没有独立前缀，路径从父字段的 key 开始。 */
    val nested: Boolean get() = prefix.isEmpty()

    companion object {
        const val NAMING_AS_IS = "AS_IS"
        const val SCOPE_BOTH = "BOTH"
    }
}

/**
 * 配置模型的一个字段。
 *
 * [key] 是已经按模型 `naming` 归一后的配置键；[declaredType] 是声明类型的全限定名
 * （`List<X>` 为 `java.util.List`），[valueType] 是嵌套解析真正要用的类型：
 * 数组/集合取元素类型、Map 取 value 类型、其余取声明类型。
 *
 * [map] 表示声明类型是 Map（含子类型）：键是实例数据（如 `method-configs.<apiId>`），
 * 不是模型字段，下探时要跳过一段。
 */
data class SpelConfigField(
    val key: String,
    val attribute: SpelAttributeMetadata,
    val declaredType: String?,
    val valueType: String?,
    val element: PsiElement,
    val map: Boolean = false
) {

    /** 结构字段：值继续按配置模型下探，本身不是表达式。 */
    val struct: Boolean get() = attribute.kind == SpelValueKind.STRUCT

    /**
     * 值中是否允许出现 SpEL 表达式：`kind` 必须在表达式白名单内。
     *
     * 白名单之外的字段（如 `url = NONE`、`retry-config = STRUCT`）运行期
     * 不会把值当作表达式求值，补全、引用、高亮与所有检查器都必须跳过。
     */
    val expressive: Boolean get() = attribute.kind in SpelValueKind.EXPRESSION_KINDS

    /**
     * 值的期望类型：显式 `resultType` 优先，否则取 Map/集合的元素类型或声明类型。
     *
     * Map 字段的元素类型就是条目值的类型，表达式写在条目上，所以直接适用。
     */
    val expectedType: String? get() = attribute.resultType ?: valueType
}

/**
 * 一个 `@SpelConfig` 模型及其字段表。
 *
 * [fields] 按配置 key 索引，包含继承自父类的字段；子类同名字段优先。
 */
data class SpelConfigModel(
    val config: SpelConfigMetadata,
    val psiClass: PsiClass,
    val fields: Map<String, SpelConfigField>
) {

    val fqName: String get() = config.fqName

    fun field(key: String): SpelConfigField? = fields[key]
}
