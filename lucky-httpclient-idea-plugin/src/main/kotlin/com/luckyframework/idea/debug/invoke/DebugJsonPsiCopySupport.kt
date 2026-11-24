package com.luckyframework.idea.debug.invoke

import com.intellij.psi.PsiFile
import java.lang.reflect.Field

/**
 * 让虚拟 JSON 文件的 PSI 副本带上原始文件。
 *
 * EditorTextField 的文档挂在 PsiFileFactory 创建的 LightVirtualFile 上。
 * 平台补全初始化会复制该 PSI 文件，并断言 `copy.originalFile == file`
 * （CompletionAssertions）：LightVirtualFile 的视图提供者不发送 PSI 事件，
 * `PsiFileImpl.clone()` 只在原文件已有 original 时才会把它继承给副本，
 * 否则副本的 original 指向自身，断言失败。
 *
 * 这里把原文件的 `PsiFileImpl.myOriginalFile` 指向自身：clone 时副本继承
 * 该引用，补全断言通过，而 `getOriginalFile()` 的返回值保持不变。
 * 不调用 `PsiFileImpl.setOriginalFile()`：它会把视图提供者注册成自己的副本，
 * 文件失效时 `markPossiblyInvalidated` 会沿副本递归。
 */
internal object DebugJsonPsiCopySupport {

    /** 平台字段名；字段不存在时保持补全的原有行为。 */
    private val ORIGINAL_FILE_FIELD: Field? = runCatching {
        Class.forName("com.intellij.psi.impl.source.PsiFileImpl")
            .getDeclaredField("myOriginalFile")
            .apply { isAccessible = true }
    }.getOrNull()

    fun bindOriginalFile(file: PsiFile) {
        val field = ORIGINAL_FILE_FIELD ?: return
        if (!field.declaringClass.isInstance(file)) {
            return
        }
        runCatching { field.set(file, file) }
    }
}
