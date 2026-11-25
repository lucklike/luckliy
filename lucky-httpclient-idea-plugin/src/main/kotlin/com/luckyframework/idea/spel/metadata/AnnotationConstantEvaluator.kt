package com.luckyframework.idea.spel.metadata

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable

/**
 * 注解属性值的编译期常量求值。
 *
 * 注解属性在内联到 class 文件时，`@Rar(Names.FILE)` 与 `@Rar("file")`
 * 对反射而言无从区分，常量求值正是复刻这一编译期行为。
 *
 * 未书写、非常量表达式或求值结果不是字符串时返回 null。
 * 平台求值器对注解属性中的常量引用一律返回 null（2024.1 实测），
 * 这里按 Java 常量表达式规则自行展开引用与拼接。
 */
object AnnotationConstantEvaluator {

    /** 读取注解属性的常量字符串值；未书写或不可静态求值时返回 null。 */
    fun stringValue(annotation: PsiAnnotation, name: String): String? =
        stringValue(annotation.findDeclaredAttributeValue(name))

    /** 读取注解属性值的常量字符串值；非常量表达式返回 null。 */
    fun stringValue(value: PsiAnnotationMemberValue?): String? {
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
}
