package com.luckyframework.idea.debug.ui

/**
 * 终端日志的 ANSI 控制序列剥离。
 *
 * 界面渲染已交给平台控制台（[ConsoleLogView]），颜色由编辑器 scheme 的
 * Console Colors 解码；这里只保留复制日志时的纯文本处理，去掉
 * `ESC[...` 形式的 CSI 序列（颜色、光标移动等）。
 */
object AnsiLogText {

    private const val ESC = '\u001B'

    /** 去掉 ANSI 控制序列后的可见文本。 */
    fun plainText(line: String): String {
        if (line.indexOf(ESC) < 0) {
            return line
        }
        val text = StringBuilder(line.length)
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            if (ch == ESC && index + 1 < line.length && line[index + 1] == '[') {
                val end = csiEnd(line, index + 2)
                if (end < 0) {
                    // 未闭合的转义序列按普通文本保留
                    text.append(ch)
                    index++
                    continue
                }
                index = end + 1
            } else {
                text.append(ch)
                index++
            }
        }
        return text.toString()
    }

    /** CSI 参数串的终止字节（0x40..0x7E），找不到说明序列未闭合。 */
    private fun csiEnd(line: String, start: Int): Int {
        var index = start
        while (index < line.length) {
            if (line[index] in '@'..'~') {
                return index
            }
            index++
        }
        return -1
    }
}
