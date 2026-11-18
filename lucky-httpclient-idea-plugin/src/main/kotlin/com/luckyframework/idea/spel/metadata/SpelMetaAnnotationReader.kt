package com.luckyframework.idea.spel.metadata

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * 在 IDE 运行期直接读取注解声明上的 SpEL 元注解。
 *
 * 数据来源是 `com.luckyframework.httpclient.proxy.spel.meta` 下的元注解：
 *
 * - `@SpelAttribute` 标注注解元素，声明 kind/stage/result/separator 等语义；
 * - `@SpelStage` / `@SpelSeparator` / `@SpelName` 标注"动态取值元素"；
 *
 * 元注解都是 `RetentionPolicy.CLASS`（写进 class 文件，PSI 可直接读），无论框架来自源码还是 jar，
 * 只要注解声明能被 PSI 解析就能读到。因此用户基于框架扩展的自定义注解
 * 无需登记到任何离线清单，同样获得补全、检查与文档能力。
 *
 * 读取结果按注解声明缓存（[CachedValuesManager]），注解声明变化后自动失效。
 * 这是注解语义的唯一数据来源，不依赖任何预编译清单。
 *
 * 读取逻辑无状态，因此全局共享同一实例：`CachedValuesManager` 以
 * 「provider 捕获上下文」校验同元素上的缓存注册，若每次 new 一个 reader，
 * 多个 inspector 在同一注解声明上注册的 provider 捕获到不同实例，
 * 平台会判定缓存不稳定并抛 `PluginException`。
 */
class SpelMetaAnnotationReader private constructor() {

    /**
     * 读取注解声明的元数据；声明上完全没有元注解时返回 null。
     */
    fun read(declaration: PsiClass): SpelAnnotationMetadata? =
        CachedValuesManager.getCachedValue(declaration) {
            CachedValueProvider.Result.create(readNow(declaration), declaration)
        }

    private fun readNow(declaration: PsiClass): SpelAnnotationMetadata? {
        val fqName = declaration.qualifiedName ?: return null
        val attributes = LinkedHashMap<String, SpelAttributeMetadata>()
        var separatorElement: String? = null
        var stageElement: String? = null
        var nameElement: String? = null

        for (method in declaration.methods) {
            val name = method.name
            if (method.hasMeta(SPEL_SEPARATOR)) {
                separatorElement = name
            }
            if (method.hasMeta(SPEL_STAGE)) {
                stageElement = name
            }
            if (method.hasMeta(SPEL_NAME)) {
                nameElement = name
            }
            val attribute = method.meta(SPEL_ATTRIBUTE) ?: continue
            attributes[name] = attributeOf(attribute)
        }

        if (attributes.isEmpty() && separatorElement == null && stageElement == null && nameElement == null) {
            return null
        }
        return SpelAnnotationMetadata(
            fqName = fqName,
            attributes = attributes,
            separatorElement = separatorElement,
            stageElement = stageElement,
            nameElement = nameElement
        )
    }

    /**
     * 读取单个元素（字段/方法）上的 `@SpelAttribute`；未标注时返回 null。
     */
    fun readAttribute(owner: PsiModifierListOwner): SpelAttributeMetadata? =
        annotationOf(owner, SPEL_ATTRIBUTE)?.let(::attributeOf)

    /**
     * 读取类上的 `@SpelConfig`；未标注时返回 null。
     */
    fun readConfig(declaration: PsiClass): SpelConfigMetadata? =
        CachedValuesManager.getCachedValue(declaration, CONFIG_KEY) {
            CachedValueProvider.Result.create(readConfigNow(declaration), declaration)
        }

    private fun readConfigNow(declaration: PsiClass): SpelConfigMetadata? {
        val annotation = annotationOf(declaration, SPEL_CONFIG) ?: return null
        return SpelConfigMetadata(
            fqName = declaration.qualifiedName ?: return null,
            prefix = stringValue(annotation.findAttributeValue("prefix")) ?: "",
            aliases = items(annotation, "aliases").mapNotNull(::stringValue),
            naming = enumName(annotation, "naming") ?: SpelConfigMetadata.NAMING_AS_IS,
            scope = enumName(annotation, "scope") ?: SpelConfigMetadata.SCOPE_BOTH,
            description = stringValue(annotation.findAttributeValue("description")) ?: ""
        )
    }

    private fun attributeOf(annotation: PsiAnnotation): SpelAttributeMetadata =
        SpelAttributeMetadata(
            kind = enumName(annotation, "kind")
                ?.let { name -> SpelValueKind.values().firstOrNull { it.name == name } }
                ?: SpelValueKind.TEMPLATE,
            lifecycle = enumName(annotation, "stage")
                ?.let { name -> SpelLifecycle.values().firstOrNull { it.name == name } }
                ?: SpelLifecycle.METHOD,
            explicit = annotation.parameterList.attributes
                .mapNotNull { it.name }
                .filter { it != VALUE_ATTRIBUTE }
                .toSet(),
            result = items(annotation, "result").mapNotNull(::classLiteralName),
            typeExpression = items(annotation, "typeExpression").mapNotNull(::stringValue),
            constraint = items(annotation, "constraint").mapNotNull(::enumReferenceName),
            separator = stringValue(annotation.findAttributeValue("separator")) ?: "",
            keySpel = booleanValue(annotation.findAttributeValue("keySpel")) ?: false,
            valueSpel = booleanValue(annotation.findAttributeValue("valueSpel")) ?: true,
            condition = stringValue(annotation.findAttributeValue("condition")) ?: "",
            key = stringValue(annotation.findAttributeValue("key")) ?: "",
            // 默认值是 Object.class，只有显式书写才代表声明了结果类型
            resultType = classLiteralName(annotation.findDeclaredAttributeValue("resultType"))
                ?.takeUnless { it == JAVA_LANG_OBJECT },
            expandable = booleanValue(annotation.findAttributeValue("expandable")) ?: false
        )

    /** 数组属性展开为元素列表；单值属性视为一个元素。 */
    private fun items(annotation: PsiAnnotation, name: String): List<PsiAnnotationMemberValue> {
        val value = annotation.findAttributeValue(name) ?: return emptyList()
        return if (value is PsiArrayInitializerMemberValue) value.initializers.toList() else listOf(value)
    }

    private fun enumName(annotation: PsiAnnotation, name: String): String? =
        enumReferenceName(annotation.findAttributeValue(name))

    /** 枚举引用 `Lifecycle.METHOD` 或静态导入的 `METHOD` 都取最后一段。 */
    private fun enumReferenceName(value: PsiAnnotationMemberValue?): String? {
        val text = when (value) {
            is PsiReferenceExpression -> value.text
            is PsiLiteralExpression -> value.value as? String
            else -> null
        } ?: return null
        return text.substringAfterLast('.').trim('"').takeIf { it.isNotEmpty() }
    }

    /** `X.class` / `String[].class` 的完整类型名；数组保持 `基类[]` 形式。 */
    private fun classLiteralName(value: PsiAnnotationMemberValue?): String? {
        val type = (value as? PsiClassObjectAccessExpression)?.operand?.type ?: return null
        return typeName(type)
    }

    private fun typeName(type: PsiType): String? = when (type) {
        is PsiArrayType -> typeName(type.componentType)?.let { "$it[]" }
        is PsiClassType -> type.resolve()?.qualifiedName ?: type.canonicalText
        else -> type.canonicalText
    }

    private fun stringValue(value: PsiAnnotationMemberValue?): String? =
        (value as? PsiLiteralExpression)?.value as? String

    private fun booleanValue(value: PsiAnnotationMemberValue?): Boolean? =
        (value as? PsiLiteralExpression)?.value as? Boolean

    /** 方法上是否标注了指定元注解；FQ 名无法解析时按简单名匹配。 */
    private fun PsiMethod.hasMeta(fqName: String): Boolean = meta(fqName) != null

    private fun PsiMethod.meta(fqName: String): PsiAnnotation? = annotationOf(this, fqName)

    /** 元素上的注解：先按 FQ 名解析，短名（未解析）时退化比较。 */
    private fun annotationOf(owner: PsiModifierListOwner, fqName: String): PsiAnnotation? {
        owner.getAnnotation(fqName)?.let { return it }
        return owner.modifierList?.annotations?.firstOrNull { it.matches(fqName) }
    }

    private fun PsiAnnotation.matches(fqName: String): Boolean {
        val qualified = qualifiedName
        if (qualified == fqName) {
            return true
        }
        // 已解析到其他包的同名注解时不匹配；只有短名（未解析）才退化比较
        if (qualified != null && qualified.contains('.')) {
            return false
        }
        val shortName = qualified ?: nameReferenceElement?.referenceName ?: return false
        return shortName == fqName.substringAfterLast('.')
    }

    companion object {
        private val INSTANCE = SpelMetaAnnotationReader()

        /** 保留 `SpelMetaAnnotationReader()` 的构造写法，永远返回同一实例。 */
        operator fun invoke(): SpelMetaAnnotationReader = INSTANCE

        private const val META_PACKAGE = "com.luckyframework.httpclient.proxy.spel.meta"
        const val SPEL_ATTRIBUTE = "$META_PACKAGE.SpelAttribute"
        const val SPEL_CONFIG = "$META_PACKAGE.SpelConfig"
        const val SPEL_SEPARATOR = "$META_PACKAGE.SpelSeparator"
        const val SPEL_STAGE = "$META_PACKAGE.SpelStage"
        const val SPEL_NAME = "$META_PACKAGE.SpelName"
        const val VALUE_ATTRIBUTE = "value"
        private const val JAVA_LANG_OBJECT = "java.lang.Object"

        /** `read` 与 `readConfig` 在同一类上共用缓存元素，必须用不同的 Key 区分。 */
        private val CONFIG_KEY: Key<CachedValue<SpelConfigMetadata?>> =
            Key.create("lucky.spel.config.metadata")
    }
}
