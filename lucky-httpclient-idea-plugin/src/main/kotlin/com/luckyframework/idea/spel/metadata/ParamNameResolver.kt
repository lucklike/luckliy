package com.luckyframework.idea.spel.metadata

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable

/**
 * 解析方法参数注入 SpEL 上下文时使用的参数名。
 *
 * 运行期参数名由 `MethodMetaContext` 计算：
 * `ParameterUtils.getParamName(parameter, asmName)` 通过
 * `AnnotationUtils.findMergedAnnotation(parameter, Param.class)`
 * 取合并后的 `@Param`，其 `value` 有文本时才覆盖编译期参数名，
 * 否则回退到 ASM/反射读到的参数名。
 *
 * 插件侧对齐 Spring `AnnotatedElementUtils.findMergedAnnotation` 的合并语义：
 * - `@Param` 既可以直接标注在参数上，也可以出现在任意层元注解上；
 * - 属性值沿 `@AliasFor` 链传递，链可以跨注解
 *   （`@Query#value` → `@DynamicParam#name` → `@Param#value`），
 *   也可以留在注解内部（`@AliasFor("value")`）；
 * - 元注解同名属性构成隐式别名（`@MyParam#value` → `@Param#value`），
 *   与 Spring 的合并规则一致；
 * - 只认源码中显式书写的属性值，注解默认值不产生覆盖；
 * - 属性值支持常量引用与常量拼接：运行期反射读到的是编译期内联后的
 *   常量值，插件侧用 `computeConstantExpression` 按同一规则静态求值。
 */
class ParamNameResolver {

    /**
     * 返回参数最终注入 SpEL 上下文时使用的名称。
     *
     * 没有 `@Param`、或合并后的 `@Param#value` 为空时返回 null，
     * 调用方应回退到编译期参数名（对应运行期的 `StringUtils.hasText` 判断）。
     */
    fun resolve(parameter: PsiParameter): String? = resolveName(parameter)?.name

    /**
     * 返回注入名以及源码中书写的该名称的位置。
     *
     * [ParamName.declaration] 指向产生名称的注解属性值（`@Param("uid")` 的
     * 字符串字面量、`@Query(NAME)` 的常量引用），供跳转定位到注解处；
     * 没有显式值时返回 null，由调用方回退到参数声明。
     */
    fun resolveName(parameter: PsiParameter): ParamName? {
        for (annotation in parameter.modifierList?.annotations ?: return null) {
            val value = mergedParamValue(annotation, HashSet(), 0)
            if (value != null && !value.name.isNullOrBlank()) {
                return value
            }
        }
        return null
    }

    /**
     * 计算该注解（含全部元注解）合并后的 `@Param#value`。
     *
     * 返回 null 表示这一支不存在 `@Param`；返回空名称表示存在 `@Param`
     * 但没有显式值。调用方继续检查其它注解，对应 Spring 的别名聚合：
     * 没有显式值的 `@Param` 仍会被兄弟注解上的别名值覆盖。
     */
    private fun mergedParamValue(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        depth: Int
    ): ParamName? {
        val typeName = annotation.qualifiedName ?: return null
        val declaration = annotation.resolveAnnotationType() ?: return null
        if (depth > MAX_DEPTH || !visited.add(typeName)) {
            return null
        }
        if (typeName == PARAM) {
            val value = annotation.findDeclaredAttributeValue(DEFAULT_ATTRIBUTE)
            return ParamName(constantValue(value) ?: "", value)
        }

        // 当前注解上显式书写、且别名链最终指向 `@Param#value` 的属性
        for (pair in annotation.parameterList.attributes) {
            val attribute = pair.name ?: DEFAULT_ATTRIBUTE
            if (!targetsParamValue(declaration, attribute, HashSet(), 0)) {
                continue
            }
            constantValue(pair.value)?.let { return ParamName(it, pair.value) }
        }

        // 就近优先：当前注解没有显式值时，逐层向元注解要值
        for (meta in declaration.modifierList?.annotations ?: return null) {
            mergedParamValue(meta, visited, depth + 1)?.let { return it }
        }
        return null
    }

    /**
     * 属性是否最终指向 `@Param#value`：显式 `@AliasFor` 链优先；
     * 没有显式别名时按 Spring 的隐式别名规则，检查元注解中的同名属性
     * （`@MyParam#value` → `@Param#value`），并支持多层传递。
     */
    private fun targetsParamValue(
        declaration: PsiClass,
        attribute: String,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        val typeName = declaration.qualifiedName ?: return false
        if (typeName == PARAM) {
            return attribute == DEFAULT_ATTRIBUTE
        }
        if (depth > MAX_DEPTH || !visited.add("$typeName#$attribute")) {
            return false
        }
        val method = declaration.findMethodsByName(attribute, false).firstOrNull() ?: return false
        // 显式别名先解析到目标属性；目标属性还可能靠元注解同名属性继续传递
        // （`@AliasFor(annotation = X.class)` → `X#value` → `@Param#value`），
        // 因此在目标属性上继续做同样的判断，而不是只比较一次名字
        findAliasFor(method)?.let { alias ->
            val targetAnnotation = declaredClass(alias, "annotation")
                ?.takeUnless { it.qualifiedName == ANNOTATION_TYPE }
            val targetAttribute = constantValue(alias, "attribute")
                ?: constantValue(alias, DEFAULT_ATTRIBUTE)
                ?: attribute
            return targetsParamValue(targetAnnotation ?: declaration, targetAttribute, visited, depth + 1)
        }
        for (meta in declaration.modifierList?.annotations ?: return false) {
            val metaFqName = meta.qualifiedName ?: continue
            if (metaFqName.startsWith("java.lang.annotation.")) {
                continue
            }
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            if (metaDeclaration.findMethodsByName(attribute, false).isEmpty()) {
                continue
            }
            if (targetsParamValue(metaDeclaration, attribute, visited, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun findAliasFor(owner: PsiModifierListOwner): PsiAnnotation? {
        owner.getAnnotation(ALIAS_FOR)?.let { return it }
        // spring-core 不在 classpath 时 FQ 名无法解析，退化为按简单名匹配
        return owner.modifierList?.annotations?.firstOrNull {
            it.qualifiedName == ALIAS_FOR || it.nameReferenceElement?.referenceName == "AliasFor"
        }
    }

    /**
     * 读取注解属性的字符串值：字面量或可静态求值的常量表达式。
     *
     * 未书写、非常量表达式或求值结果不是字符串时返回 null。
     * 注解属性在内联到 class 文件时，`@Query(NAME)` 与 `@Query("req")`
     * 对反射而言无从区分，常量求值正是复刻这一编译期行为。
     */
    private fun constantValue(annotation: PsiAnnotation, name: String): String? =
        constantValue(annotation.findDeclaredAttributeValue(name))

    private fun constantValue(value: PsiAnnotationMemberValue?): String? {
        if (value == null) {
            return null
        }
        if (value is PsiLiteralExpression) {
            return value.value as? String
        }
        if (value !is PsiReferenceExpression) {
            // 常量拼接等纯运算表达式：平台求值器可以处理
            (JavaPsiFacade.getInstance(value.project)
                .constantEvaluationHelper
                .computeConstantExpression(value) as? String)?.let { return it }
        }
        // 平台求值器对注解属性中的常量引用一律返回 null（2024.1 实测），
        // 这里按 Java 常量表达式规则自行展开引用与拼接
        return declaredConstant(value, HashSet())
    }

    private fun declaredConstant(value: PsiElement?, visited: MutableSet<PsiElement>): String? {
        if (value == null || !visited.add(value)) {
            return null
        }
        return when (value) {
            // 拼接中的基本类型常量按 Java 的字符串转换规则取值（"total_" + 10 → "total_10"）
            is PsiLiteralExpression -> value.value?.toString()
            is PsiReferenceExpression -> {
                val variable = value.resolve() as? PsiVariable ?: return null
                // 来自 class 文件的常量没有 initializer，取编译器写入的 ConstantValue
                if (variable is PsiField) {
                    (variable.computeConstantValue() as? String)?.let { return it }
                }
                declaredConstant(variable.initializer, visited)
            }
            is PsiPolyadicExpression -> {
                if (value.operationTokenType != JavaTokenType.PLUS) {
                    return null
                }
                // 每个操作数只继承祖先链，同一常量在拼接中可重复出现
                val parts = value.operands.map { declaredConstant(it, HashSet(visited)) ?: return null }
                parts.joinToString("")
            }
            is PsiParenthesizedExpression -> declaredConstant(value.expression, visited)
            is PsiTypeCastExpression -> declaredConstant(value.operand, visited)
            else -> null
        }
    }

    /** 解析 `@AliasFor(annotation = X.class)` 中的注解类型。 */
    private fun declaredClass(annotation: PsiAnnotation, name: String): PsiClass? {
        val value = annotation.findDeclaredAttributeValue(name) as? PsiClassObjectAccessExpression ?: return null
        val classType = value.operand.type as? PsiClassType ?: return null
        return classType.resolve()
    }

    companion object {
        private const val PARAM = "com.luckyframework.reflect.Param"
        private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        private const val ANNOTATION_TYPE = "java.lang.annotation.Annotation"
        private const val DEFAULT_ATTRIBUTE = "value"
        private const val MAX_DEPTH = 16
    }
}

/**
 * 参数注入名及其源码位置。
 *
 * [declaration] 是注解中书写名称的属性值：字符串字面量可以直接跳过起始
 * 引号定位到名称前，常量引用则定位到引用表达式起点；为 null 表示名称
 * 来自参数声明本身。
 */
data class ParamName(
    val name: String,
    val declaration: PsiAnnotationMemberValue? = null
)
