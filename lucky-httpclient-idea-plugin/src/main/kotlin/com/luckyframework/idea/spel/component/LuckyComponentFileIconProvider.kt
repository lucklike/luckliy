package com.luckyframework.idea.spel.component

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import javax.swing.Icon

/**
 * `@LuckyComponent` 组件类在编辑器标签页等基于文件的位置显示品牌图标。
 *
 * 项目树、结构视图等基于 PSI 元素的位置由 [LuckyComponentIconProvider] 处理；
 * 编辑器标签页只拿得到 [VirtualFile]，平台通过 [FileIconProvider] 扩展点取图标，
 * 这里按文件内是否有组件类返回同一套图标（同样受设置页开关与自定义图标控制）。
 *
 * 组件身份来自 [com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols] 的注解扫描，
 * 与补全/检查同源；dumb 模式下扫描结果为空，回退平台默认的 Java 文件图标。
 */
class LuckyComponentFileIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if (project == null || project.isDisposed || !file.isValid) {
            return null
        }
        if (file.extension != "java") {
            return null
        }
        val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile ?: return null
        val declared = psiFile.classes.any { isLuckyComponentClass(it) }
        return if (declared) ComponentIconResolver.icon() else null
    }
}
