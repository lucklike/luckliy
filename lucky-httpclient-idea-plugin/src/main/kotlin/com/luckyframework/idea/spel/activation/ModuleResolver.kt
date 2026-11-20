package com.luckyframework.idea.spel.activation

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * 元素所属模块的解析。
 *
 * `ModuleUtilCore.findModuleForPsiElement` 依赖 `ProjectFileIndex`（WorkspaceFileIndex）：
 * 索引数据尚未就绪时在 EDT 上会触发 `Slow operations are prohibited on EDT`。
 * 补全、意图、折叠与行标记都在 EDT 上判定模块，所以这里只读模块模型与内容根
 * （静态结构，任何线程都安全），不触碰文件索引；内容根嵌套时取最长（最内层）匹配。
 *
 * 依赖库（jar）中的文件没有内容根，返回 null，与索引查询的结果一致。
 */
object ModuleResolver {

    fun moduleOf(element: PsiElement): Module? =
        element.containingFile?.virtualFile?.let { moduleOf(element.project, it) }

    fun moduleOf(project: Project, file: VirtualFile): Module? {
        val path = file.path
        var matched: Module? = null
        var matchedLength = -1
        for (module in ModuleManager.getInstance(project).modules) {
            for (root in ModuleRootManager.getInstance(module).contentRoots) {
                val rootPath = root.path
                if (rootPath.length <= matchedLength || !isInside(path, rootPath)) {
                    continue
                }
                matched = module
                matchedLength = rootPath.length
            }
        }
        return matched
    }

    /** VFS 路径统一用 `/` 分隔；内容根自身也算命中。 */
    private fun isInside(path: String, rootPath: String): Boolean {
        if (path == rootPath) {
            return true
        }
        return path.length > rootPath.length &&
            path.startsWith(rootPath) &&
            path[rootPath.length] == '/'
    }
}
