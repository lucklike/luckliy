package com.luckyframework.idea.std

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * `@StdHttpClient` 缺失配置时的「生成配置」入口。
 *
 * 写入细节由 [StdConfigWriter] 扩展点实现（YAML 侧），这里只负责目标文件
 * 选择与结果提示。未安装 YAML 插件或索引不可用时入口不出现。
 */
object StdConfigGenerator {

    fun isAvailable(project: Project): Boolean =
        StdConfigWriter.instance() != null && !DumbService.isDumb(project)

    fun candidates(project: Project): List<PsiFile> =
        StdConfigWriter.instance()?.candidates(project).orEmpty()

    /** 写入指定文件；返回是否发生了写入。 */
    fun generate(file: PsiFile, configId: String): Boolean =
        StdConfigWriter.instance()?.write(file, configId) == true

    /**
     * 弹窗选择目标文件后生成。
     *
     * 项目内没有配置文件时直接新建 `application.yml`；列表末尾始终提供
     * 「新建 application.yml」，便于把配置落到独立文件。
     */
    fun chooseTarget(project: Project, editor: Editor?, configId: String) {
        if (!isAvailable(project)) {
            return
        }
        val writer = StdConfigWriter.instance() ?: return
        val candidates = writer.candidates(project)
        if (candidates.isEmpty()) {
            createAndWrite(project, writer, configId)
            return
        }
        val items = ArrayList<Target>(candidates.size + 1)
        candidates.forEach { items.add(Target(it, label(project, it))) }
        items.add(Target(null, "新建 application.yml"))

        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setTitle("生成 StdHttpClient 配置：$configId")
            .setRenderer(object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    text = (value as? Target)?.label.orEmpty()
                    return this
                }
            })
            .setItemChosenCallback(Consumer<Target> { target ->
                val file = target.file
                if (file != null) {
                    writeAndOpen(project, file, configId)
                } else {
                    createAndWrite(project, writer, configId)
                }
            })
            .createPopup()
        if (editor != null) {
            popup.showInBestPositionFor(editor)
        } else {
            popup.showInFocusCenter()
        }
    }

    private fun createAndWrite(project: Project, writer: StdConfigWriter, configId: String) {
        val file = writer.createDefaultFile(project) ?: return
        writeAndOpen(project, file, configId)
    }

    private fun writeAndOpen(project: Project, file: PsiFile, configId: String) {
        if (!generate(file, configId)) {
            return
        }
        val virtualFile = file.virtualFile ?: return
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    private fun label(project: Project, file: PsiFile): String {
        val virtualFile = file.virtualFile ?: return file.name
        val basePath = project.basePath ?: return file.name
        val base = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return file.name
        return VfsUtilCore.getRelativePath(virtualFile, base, '/') ?: file.name
    }

    /** 写入目标；[file] 为 null 表示新建 `application.yml`。 */
    private class Target(val file: PsiFile?, val label: String) {
        override fun toString(): String = label
    }
}
