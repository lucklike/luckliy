package com.luckyframework.idea.spel.reference

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.pom.Navigatable
import com.luckyframework.idea.spel.documentation.SpelDocumentation
import javax.swing.Icon

/**
 * SpEL 跳转目标（design D8）。
 *
 * [target] 存在时定位到真实 PSI 的 [sourceRange]（相对 [target]）位置；
 * 内置符号没有库源码，只提供名称与说明，`canNavigateToSource` 为 false。
 * [documentation] 是悬停文档内容，未解析的虚拟声明通过 [kindLabel] 标记。
 * [documentationTarget] 是文档渲染目标：跳转目标可能落在 `@Param("uid")` 的
 * 属性值等没有文档的 PSI 上，此时用它指向真正可展示的声明（如方法参数），
 * 由平台原生文档链生成与 IDEA 内置一致的内容。
 * [anchor] 是引用所在元素，作为虚拟声明的父元素：平台为悬停创建
 * SmartPointer 时会校验 `parent != null`，缺少父元素会导致文档不可用。
 * [isType] 标记 `T(Type)` 等类型引用：`@bean` 的跳转目标同样是类，但 Bean 名
 * 不随类重命名变化，不能算作对类的引用。
 */
class SpelDeclaration(
    private val project: Project,
    private val name: String,
    val kindLabel: String,
    private val typeLabel: String? = null,
    private val description: String? = null,
    val target: PsiElement? = null,
    private val sourceRange: TextRange? = null,
    val documentation: SpelDocumentation? = null,
    private val anchor: PsiElement? = null,
    val documentationTarget: PsiElement? = null,
    val isType: Boolean = false
) : FakePsiElement(), Navigatable {

    /**
     * 父元素不能为 null：平台 `PsiUtilCore.ensureValid` 会为没有父元素的 PSI 抛
     * `PsiInvalidElementAccessException`，文档兜底路径正是用它创建 SmartPointer。
     */
    override fun getParent(): PsiElement? =
        anchor ?: target?.parent ?: target ?: getContainingFile()

    override fun getProject(): Project = project

    /** 不依赖 [getContainingFile]；未解析的内置符号没有物理文件，仍需可用的管理器。 */
    override fun getManager(): PsiManager = PsiManager.getInstance(project)

    /** 虚拟声明按 Java 语言的文档提供器渲染。 */
    override fun getLanguage(): Language = JavaLanguage.INSTANCE

    override fun getName(): String = name

    override fun getText(): String = name

    /**
     * 虚拟声明所属文件始终返回**原始文件**。
     *
     * 补全时 [anchor] 位于副本文件，而 `CompletionUtil.getOriginalElement`
     * 会拿副本文件去原文件里按声明类型查找同偏移元素：虚拟声明在原文件里
     * 并不存在，查找失败会返回 null，导致 2026.3 的补全文档目标收集为空。
     * 返回原文件后该查找判定为「已是原文件」并原样返回声明自身。
     */
    override fun getContainingFile(): PsiFile? =
        anchor?.containingFile?.originalFile ?: target?.containingFile?.originalFile

    /** 查找用法按项目范围搜索，保证跨文件的 `#{}` 引用都能被收集。 */
    override fun getUseScope(): SearchScope = GlobalSearchScope.projectScope(project)

    /**
     * 声明位置：始终落在 [getContainingFile] 返回的文件内。
     *
     * 与 [target] 不在同一文件时返回锚点偏移；否则返回 [targetOffset]。
     * 补全副本里 [target] 常被解析为原始文件实例，两者不一致时用锚点
     * 偏移，否则平台会拿外部文件的偏移到当前文档取行号，例如补全 ML
     * 排序器的 `linesDiff` 会因此抛 `IndexOutOfBoundsException`。
     */
    override fun getTextOffset(): Int {
        val containing = getContainingFile()
        val declaration = target
        if (containing == null || declaration == null) {
            return anchor?.textOffset ?: 0
        }
        if (declaration.containingFile !== containing) {
            return anchor?.textOffset ?: 0
        }
        return targetOffset()
    }

    /**
     * 跳转偏移：始终是 [target] 在其自身文件中的位置。
     *
     * Java 方法/类/字段的 `textRange` 起点包含 Javadoc 注释，平台默认导航
     * 用的正是 `textOffset`。因此：
     * - 没有显式 [sourceRange]（声明本身）时使用 `textOffset`；
     * - 空范围不携带内容，语义同“声明本身”，同样使用 `textOffset`；
     * - 只有字面量内的显式偏移（`@Param("uid")` 跳过起始引号）才按
     *   `textRange.startOffset + startOffset` 计算。
     */
    private fun targetOffset(): Int {
        val declaration = target ?: return 0
        val range = sourceRange
        if (range == null || (range.isEmpty && range.startOffset == 0)) {
            return declaration.textOffset
        }
        return declaration.textRange.startOffset + range.startOffset
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = target != null

    override fun navigate(requestFocus: Boolean) {
        val file = target?.containingFile ?: return
        val virtualFile = file.virtualFile ?: return
        OpenFileDescriptor(project, virtualFile, targetOffset()).navigate(requestFocus)
    }

    override fun getPresentation(): ItemPresentation = this

    override fun getPresentableText(): String = name

    override fun getLocationString(): String = buildString {
        append(kindLabel)
        if (!typeLabel.isNullOrBlank()) {
            append(" · ").append(typeLabel)
        }
        if (!description.isNullOrBlank()) {
            append(" · ").append(description)
        }
    }

    override fun getIcon(unused: Boolean): Icon? = null

    override fun equals(other: Any?): Boolean =
        other is SpelDeclaration &&
            other.name == name &&
            other.kindLabel == kindLabel &&
            other.target == target

    override fun hashCode(): Int = 31 * name.hashCode() + kindLabel.hashCode()

    override fun toString(): String = "$kindLabel $name"
}
