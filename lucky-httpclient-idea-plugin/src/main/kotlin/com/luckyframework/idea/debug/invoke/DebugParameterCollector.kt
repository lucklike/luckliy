package com.luckyframework.idea.debug.invoke

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiWildcardType
import com.luckyframework.idea.spel.metadata.ParamNameResolver

/**
 * 把 PsiMethod 的参数转换成调试侧的类型模型。
 *
 * 参数名与运行期对齐：优先 @Param 合并值，缺失时回退到编译期参数名。
 * 类型名使用擦除泛型后的全限定名，供 launcher 定位重载方法。
 */
class DebugParameterCollector(
    private val names: ParamNameResolver = ParamNameResolver()
) {

    fun collect(method: PsiMethod): List<DebugParameter> =
        method.parameterList.parameters.mapIndexed { index, parameter ->
            val type = parameter.type
            val enumClass = enumClassOf(type)
            val unsupported = unsupportedReasonOf(type)
            DebugParameter(
                index = index,
                name = names.resolve(parameter) ?: parameter.name,
                typeName = erasedName(type),
                kind = kindOf(type, enumClass, unsupported),
                enumConstants = enumConstantsOf(enumClass),
                unsupportedReason = unsupported
            )
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
        "java.io.InputStream", "java.io.Reader" -> "P1 不支持流类型参数"
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
}
