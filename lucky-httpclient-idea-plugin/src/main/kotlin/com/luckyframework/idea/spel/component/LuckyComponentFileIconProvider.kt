package com.luckyframework.idea.spel.component

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * `@LuckyComponent` 组件类在编辑器标签页等基于文件的位置显示品牌图标。
 *
 * 项目树、结构视图等基于 PSI 元素的位置由 [LuckyComponentIconProvider] 处理；
 * 编辑器标签页只拿得到 [VirtualFile]，平台通过 [FileIconProvider] 扩展点取图标
 * （同样受设置页开关与自定义图标控制）。
 *
 * 文件图标在 EDT 的树/标签页绘制中获取，2026.3 的绘制路径没有读锁，
 * 因此这里**不解析 PSI**，只看文件名：Java 公共类要求类名与文件名一致，
 * 按组件清单的简单名匹配即可，既不阻塞也不会抛读权限断言。
 */
class LuckyComponentFileIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if (project == null || project.isDisposed || !file.isValid) {
            return null
        }
        if (file.extension != "java") {
            return null
        }
        if (!ComponentNameCache.names(project).matchesSimpleName(file.nameWithoutExtension)) {
            return null
        }
        return ComponentIconResolver.icon()
    }
}
