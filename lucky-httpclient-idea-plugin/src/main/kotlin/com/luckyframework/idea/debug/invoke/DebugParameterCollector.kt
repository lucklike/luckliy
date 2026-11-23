package com.luckyframework.idea.debug.invoke

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.javadoc.PsiDocComment
import com.luckyframework.idea.spel.metadata.ParamNameResolver

/**
 * 把 PsiMethod 的参数转换成调试侧的类型模型。
 *
 * 参数名与运行期对齐：优先 @Param 合并值，缺失时回退到编译期参数名。
 * 类型名使用擦除泛型后的全限定名，供 launcher 定位重载方法。
 *
 * 对象参数会递归展开非静态字段（record 用 record component），列表记录元素结构，
 * 供表单直接生成子输入框、JSON 模式生成带注释的骨架；展开有深度与环检测，
 * JDK/Kotlin 内置类型不展开，避免把 LocalDate 的内部字段画到表单上。
 */
class DebugParameterCollector(
    private val names: ParamNameResolver = ParamNameResolver()
) {

    fun collect(method: PsiMethod): List<DebugParameter> {
        val methodDoc = method.docComment
        return method.parameterList.parameters.mapIndexed { index, parameter ->
            buildParameter(
                index = index,
                name = names.resolve(parameter) ?: parameter.name,
                type = parameter.type,
                description = paramDescription(methodDoc, parameter.name),
                visited = emptySet(),
                depth = 0,
                required = true
            )
        }
    }

    private fun buildParameter(
        index: Int,
        name: String,
        type: PsiType,
        description: String?,
        visited: Set<String>,
        depth: Int,
        required: Boolean
    ): DebugParameter {
        val enumClass = enumClassOf(type)
        val unsupported = unsupportedReasonOf(type)
        val kind = kindOf(type, enumClass, unsupported)
        return DebugParameter(
            index = index,
            name = name,
            typeName = erasedName(type),
            kind = kind,
            enumConstants = enumConstantsOf(enumClass),
            required = required,
            unsupportedReason = unsupported,
            description = description,
            fields = if (kind == DebugParameterKind.OBJECT) fieldsOf(type, visited, depth) else emptyList(),
            element = if (kind == DebugParameterKind.LIST) elementOf(type, visited, depth) else null
        )
    }

    /**
     * 对象字段：record 优先用 record component，普通类用含继承的全部字段。
     *
     * 静态字段与 serialVersionUID 不参与反序列化，直接跳过；
     * 同名的泛型嵌套类型通过 [visited] 断开环，避免无限展开。
     */
    private fun fieldsOf(type: PsiType, visited: Set<String>, depth: Int): List<DebugParameter> {
        val psiClass = (type as? PsiClassType)?.resolve() ?: return emptyList()
        if (psiClass.isEnum || psiClass.isAnnotationType || !isUserType(psiClass)) {
            return emptyList()
        }
        val qualified = psiClass.qualifiedName ?: return emptyList()
        if (depth >= MAX_FIELD_DEPTH || qualified in visited) {
            return emptyList()
        }
        val nestedVisited = visited + qualified
        val recordDoc = psiClass.docComment
        val components = psiClass.recordComponents
        if (components.isNotEmpty()) {
            return components.mapIndexed { index, component ->
                buildField(
                    index = index,
                    name = component.name,
                    type = component.type,
                    description = descriptionOf(component, paramDescription(recordDoc, component.name)),
                    visited = nestedVisited,
                    depth = depth + 1
                )
            }
        }
        return psiClass.allFields
            .filter { field ->
                !field.hasModifierProperty(PsiModifier.STATIC) &&
                    field.name != "serialVersionUID" &&
                    !field.name.contains('$')
            }
            .mapIndexed { index, field ->
                buildField(
                    index = index,
                    name = field.name,
                    type = field.type,
                    description = descriptionOf(field, null),
                    visited = nestedVisited,
                    depth = depth + 1
                )
            }
    }

    private fun buildField(
        index: Int,
        name: String,
        type: PsiType,
        description: String?,
        visited: Set<String>,
        depth: Int
    ): DebugParameter = buildParameter(
        index = index,
        name = name,
        type = type,
        description = description,
        visited = visited,
        depth = depth,
        required = false
    )

    /** 列表元素结构；数组/可变参数取组件类型，容器取首个类型参数。 */
    private fun elementOf(type: PsiType, visited: Set<String>, depth: Int): DebugParameter? {
        val elementType = when (type) {
            is PsiEllipsisType -> type.componentType
            is PsiArrayType -> type.componentType
            is PsiClassType -> type.parameters.firstOrNull()
            else -> null
        } ?: return null
        return buildParameter(
            index = -1,
            name = "element",
            type = elementType,
            description = null,
            visited = visited,
            depth = depth + 1,
            required = false
        )
    }

    /** JDK/Kotlin 内置类型不展开字段：反序列化用的是转换器，不是字段名。 */
    private fun isUserType(psiClass: PsiClass): Boolean {
        val qualified = psiClass.qualifiedName ?: return false
        return NON_EXPANDABLE_PACKAGES.none { qualified.startsWith(it) }
    }

    /** 字段/参数说明：Javadoc 优先，其次常见文档注解。 */
    private fun descriptionOf(member: PsiElement, doc: String?): String? {
        if (!doc.isNullOrBlank()) {
            return doc
        }
        val docText = docTextOf((member as? PsiDocCommentOwner)?.docComment)
        if (!docText.isNullOrBlank()) {
            return docText
        }
        val annotations = (member as? PsiModifierListOwner)?.modifierList?.annotations
        for (annotation in annotations.orEmpty()) {
            val attribute = DOC_ANNOTATIONS[annotation.qualifiedName] ?: continue
            val literal = annotation.findAttributeValue(attribute) as? PsiLiteralExpression
            val text = literal?.value as? String
            if (!text.isNullOrBlank()) {
                return text.trim()
            }
        }
        return null
    }

    private fun docTextOf(doc: PsiDocComment?): String? =
        doc?.descriptionElements
            ?.joinToString("") { it.text }
            ?.replace(WHITESPACE, " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** Javadoc 的 @param 说明；名称按源码参数名匹配，@Param 重命名不影响。 */
    private fun paramDescription(doc: PsiDocComment?, name: String): String? {
        val tag = doc?.findTagsByName("param")
            ?.firstOrNull { it.valueElement?.text == name }
            ?: return null
        // dataElements 的首项就是参数名，说明从第二项开始
        return tag.dataElements
            .drop(1)
            .joinToString("") { it.text }
            .replace(WHITESPACE, " ")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun kindOf(
        type: PsiType,
        enumClass: PsiClass?,
        unsupported: String?
    ): DebugParameterKind {
        if (unsupported != null) {
            return DebugParameterKind.UNSUPPORTED
        }
        if (enumClass != null) {
            return DebugParameterKind.ENUM
        }
        if (type is PsiArrayType) {
            return if (type.componentType is PsiPrimitiveType && type.componentType.canonicalText == "byte") {
                DebugParameterKind.BYTES
            } else {
                DebugParameterKind.LIST
            }
        }
        return when (erasedName(type)) {
            "java.lang.String" -> DebugParameterKind.STRING
            "boolean", "java.lang.Boolean" -> DebugParameterKind.BOOLEAN
            "byte", "short", "int",
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer" -> DebugParameterKind.INT
            "long", "java.lang.Long" -> DebugParameterKind.LONG
            "float", "double", "java.lang.Float", "java.lang.Double" -> DebugParameterKind.DOUBLE
            "char", "java.lang.Character" -> DebugParameterKind.CHAR
            "java.math.BigDecimal" -> DebugParameterKind.DECIMAL
            "java.math.BigInteger" -> DebugParameterKind.BIGINT
            "java.io.File", "java.nio.file.Path" -> DebugParameterKind.FILE
            // 流参数统一按字节读取：表单/JSON 里填 Base64 或选择本地文件
            "java.io.InputStream", "java.io.Reader" -> DebugParameterKind.BYTES
            "java.util.List", "java.util.Set", "java.util.Collection", "java.lang.Iterable" ->
                DebugParameterKind.LIST
            "java.util.Map" -> DebugParameterKind.MAP
            else -> DebugParameterKind.OBJECT
        }
    }

    private fun enumClassOf(type: PsiType): PsiClass? {
        val resolved = (type as? PsiClassType)?.resolve() ?: return null
        return if (resolved.isEnum) resolved else null
    }

    private fun enumConstantsOf(enumClass: PsiClass?): List<String> =
        enumClass?.fields
            ?.filterIsInstance<PsiEnumConstant>()
            ?.mapNotNull { it.name }
            ?: emptyList()

    private fun unsupportedReasonOf(type: PsiType): String? = when (erasedName(type)) {
        "org.springframework.web.multipart.MultipartFile" -> "P1 不支持文件上传参数"
        "org.springframework.beans.factory.ObjectProvider" -> "P1 不支持 ObjectProvider 参数"
        else -> null
    }

    /**
     * 擦除泛型后的类型名。
     *
     * 数组保留 `[]` 后缀，可变参数与数组同样处理；
     * 类型变量与通配符一律退化为 Object，由 launcher 按 JSON 转换。
     */
    private fun erasedName(type: PsiType): String = when (type) {
        is PsiPrimitiveType -> type.canonicalText
        is PsiEllipsisType -> erasedName(type.componentType) + "[]"
        is PsiArrayType -> erasedName(type.componentType) + "[]"
        is PsiClassType -> when (val resolved = type.resolve()) {
            // 类型变量在 PSI 中是解析到 PsiTypeParameter 的 PsiClassType，
            // qualifiedName 为 null，必须在这里优先擦除为 Object
            is PsiTypeParameter -> "java.lang.Object"
            else -> resolved?.qualifiedName ?: type.canonicalText.substringBefore('<')
        }
        is PsiWildcardType -> type.extendsBound?.let { erasedName(it) } ?: "java.lang.Object"
        is PsiTypeParameter -> "java.lang.Object"
        else -> type.canonicalText.substringBefore('<')
    }

    private companion object {
        /** 字段递归深度上限：足够覆盖常见 DTO 嵌套，又能让失控的模型停下来。 */
        private const val MAX_FIELD_DEPTH = 3

        /** 这些包的字段是内部实现，反序列化不按字段名匹配，不展开。 */
        private val NON_EXPANDABLE_PACKAGES =
            listOf("java.", "javax.", "jdk.", "sun.", "com.sun.", "kotlin.", "org.jetbrains.")

        /** 常见接口文档注解：属性名 → 可读说明。 */
        private val DOC_ANNOTATIONS = mapOf(
            "io.swagger.v3.oas.annotations.media.Schema" to "description",
            "io.swagger.v3.oas.annotations.Parameter" to "description",
            "io.swagger.annotations.ApiModelProperty" to "value",
            "io.swagger.annotations.ApiParam" to "value"
        )

        private val WHITESPACE = Regex("\\s+")
    }
}
