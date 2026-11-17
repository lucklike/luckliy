package com.luckyframework.idea.spel.intention

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.evaluator.EvaluatorClassLoader
import com.luckyframework.idea.spel.evaluator.EvaluatorLoadResult
import com.luckyframework.idea.spel.evaluator.EvaluatorResult
import com.luckyframework.idea.spel.evaluator.EvaluatorUnavailableException
import com.luckyframework.idea.spel.evaluator.SpelEvaluationFormatter
import com.luckyframework.idea.spel.evaluator.SpelEvaluationInput
import com.luckyframework.idea.spel.evaluator.SpelEvaluator
import com.luckyframework.idea.spel.evaluator.SpringEvaluatorEngine
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.model.SpelFragment
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 「求值 SpEL」（15.3）。
 *
 * 光标或选区位于字符串字面量的 `#{...}` 片段内时可用：
 * - 表达式引用 `#name` 变量时弹出对话框收集模拟值；
 * - 从模块 classpath 反射加载 `spring-expression`，在后台受控求值；
 * - 结果显示在消息框中，失败只提示原因，不改动源文件（15.4）。
 */
class SpelEvaluateIntention : BaseIntentionAction() {

    override fun getText(): String = "求值 SpEL"

    override fun getFamilyName(): String = "SpEL 表达式"

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val literal = stringLiteralAt(editor, file) ?: return false
        if (!LuckyHttpClientPresence.getInstance(project).isEnabledFor(literal)) {
            return false
        }
        // 未标注 @SpelAttribute 的属性运行期不求值，不能求值
        if (!SpelAnnotationResolver().isSpelLiteral(literal)) {
            return false
        }
        return fragmentAt(editor, literal) != null
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val literal = stringLiteralAt(editor, file) ?: return
        val fragment = fragmentAt(editor, literal) ?: return
        val expression = fragment.content

        val variables = variablesOf(project, expression) ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(literal)
        if (module == null) {
            Messages.showWarningDialog(project, "无法确定当前文件所属的模块，无法求值。", TITLE)
            return
        }

        val result = try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously<EvaluatorResult, RuntimeException>(
                { evaluate(module, expression, variables) },
                "正在求值 SpEL 表达式…",
                true,
                project
            )
        } catch (e: ProcessCanceledException) {
            return
        }

        val message = SpelEvaluationFormatter.format(result)
        when (result) {
            is EvaluatorResult.Success -> Messages.showInfoMessage(project, message, TITLE)
            is EvaluatorResult.MissingVariable,
            is EvaluatorResult.Timeout,
            is EvaluatorResult.Forbidden -> Messages.showWarningDialog(project, message, TITLE)

            is EvaluatorResult.Failure,
            is EvaluatorResult.Unavailable -> Messages.showErrorDialog(project, message, TITLE)
        }
    }

    /** 需要模拟变量时返回用户输入（取消对话框返回 null）。 */
    private fun variablesOf(project: Project, expression: String): Map<String, Any?>? {
        val referenced = SpelEvaluationInput.referencedVariables(expression)
        if (referenced.isEmpty()) {
            return emptyMap()
        }
        val dialog = SpelEvaluateDialog(project, expression, referenced)
        if (!dialog.showAndGet()) {
            return null
        }
        return SpelEvaluationInput.parseValues(dialog.valuesText)
    }

    private fun evaluate(
        module: Module,
        expression: String,
        variables: Map<String, Any?>
    ): EvaluatorResult {
        val load = ReadAction.compute<EvaluatorLoadResult, RuntimeException> {
            EvaluatorClassLoader.fromModule(module).load()
        }
        return when (load) {
            is EvaluatorLoadResult.Missing -> EvaluatorResult.Unavailable(load.message)
            is EvaluatorLoadResult.Available -> try {
                SpelEvaluator(SpringEvaluatorEngine(load.loader)).evaluate(expression, variables)
            } catch (e: EvaluatorUnavailableException) {
                EvaluatorResult.Unavailable(e.message ?: "无法加载 spring-expression")
            } catch (e: LinkageError) {
                EvaluatorResult.Unavailable("spring-expression 版本不兼容：${e.message}")
            } catch (e: Exception) {
                EvaluatorResult.Failure(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** 光标/选区所在的片段；嵌套时取最内层。偏移均相对字面量源码文本。 */
    private fun fragmentAt(editor: Editor, literal: PsiLiteralExpression): SpelFragment? {
        val model = editor.selectionModel
        val start = if (model.hasSelection()) model.selectionStart else editor.caretModel.offset
        val end = if (model.hasSelection()) model.selectionEnd else start
        val from = start - literal.textRange.startOffset
        val to = end - literal.textRange.startOffset
        if (to < 0) {
            return null
        }

        fun find(fragments: List<SpelFragment>): SpelFragment? {
            for (fragment in fragments) {
                find(fragment.children)?.let { return it }
                val range = fragment.textRange
                val contains = if (from == to) {
                    from >= range.startOffset && from <= range.endOffset
                } else {
                    range.containsRange(from, to)
                }
                if (contains) {
                    return fragment
                }
            }
            return null
        }
        return find(SpelScanner().scanLiteral(literal.text))
    }

    private companion object {
        const val TITLE = "求值 SpEL"
    }
}

/**
 * 模拟变量输入（15.3）。
 *
 * 每行一个 `name=value`，引用到的变量已预填名称；`#root`/`#this` 不在此列。
 */
class SpelEvaluateDialog(
    project: Project,
    private val expression: String,
    referenced: List<String>
) : DialogWrapper(project) {

    private val editor = JBTextArea(referenced.joinToString("\n") { "$it=" }, 6, 48)

    val valuesText: String
        get() = editor.text

    init {
        title = "求值 SpEL"
        init()
    }

    override fun createCenterPanel(): JComponent = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
        add(
            JBLabel(
                "<html>表达式：<b>${escape(expression)}</b><br>" +
                    "模拟变量（每行一个 name=value）：</html>"
            ),
            BorderLayout.NORTH
        )
        add(JBScrollPane(editor), BorderLayout.CENTER)
        preferredSize = Dimension(JBUI.scale(480), JBUI.scale(220))
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
