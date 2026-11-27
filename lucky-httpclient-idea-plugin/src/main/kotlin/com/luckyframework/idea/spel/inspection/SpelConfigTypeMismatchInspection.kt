package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.yaml.SpelYamlContextBuilder
import com.luckyframework.idea.spel.yaml.SpelYamlSupport
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 配置值中表达式结果类型与 `@SpelConfig` 字段期望类型的兼容性检查。
 *
 * 只对命中配置模型的字段生效：标量所在键路径先映射到 `@SpelConfig`
 * 字段（[SpelYamlContextBuilder]），拿到 `resultType`/`valueType` 声明的
 * 期望类型后，用 [com.luckyframework.idea.spel.completion.SpelTypeResolver.resultType]
 * 推断表达式结果类型。判定范围与原因见 [SpelConfigTypeMismatchChecker]。
 *
 * 字段 `kind` 不在表达式白名单内时上下文未知，不参与检查；路径未命中
 * 配置模型时没有期望类型，同样不检查，避免对自定义配置误报。
 *
 * 检查注册在 `yaml-spel-support.xml`：类引用 YAML PSI，未安装 YAML 插件时
 * 不加载。
 */
class SpelConfigTypeMismatchInspection : LocalInspectionTool() {

    private val contextBuilder = SpelYamlContextBuilder()

    private val checker = SpelConfigTypeMismatchChecker()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val scalar = element as? YAMLScalar ?: return
                inspect(holder, scalar)
            }
        }

    private fun inspect(holder: ProblemsHolder, scalar: YAMLScalar) {
        if (!SpelYamlSupport.isExpressionValue(scalar)) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(scalar.project).isEnabledFor(scalar)) {
            return
        }
        val context = contextBuilder.build(scalar)
        if (!context.known || context.expectedType == null) {
            return
        }
        for (problem in checker.check(scalar.text, context, scalar.project, scalar)) {
            holder.registerProblem(
                scalar,
                problem.message,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                problem.range
            )
        }
    }
}
