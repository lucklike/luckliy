package com.luckyframework.idea.debug.invoke

import com.intellij.json.JsonFileType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 虚拟 JSON 文件的副本必须带回原始文件：平台补全初始化会断言
 * `copy.originalFile == file`，LightVirtualFile 的副本默认不继承 original。
 */
class DebugJsonPsiCopySupportTest : BasePlatformTestCase() {

    fun `test psi copy inherits original file after binding`() {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("lucky-arguments.json", JsonFileType.INSTANCE, """{"city":"shenzhen"}""")

        // 未绑定时副本的 original 指向副本自身，平台补全断言会失败
        assertSame(file, file.originalFile)
        assertNotSame(file, (file.copy() as PsiFile).originalFile)

        DebugJsonPsiCopySupport.bindOriginalFile(file)

        val copy = file.copy() as PsiFile
        assertSame(file, copy.originalFile)
        // 原文件的 original 仍是自身，外部行为不变
        assertSame(file, file.originalFile)
    }
}
