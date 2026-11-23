package com.luckyframework.idea.debug.invoke

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/**
 * 严格 JSON 校验，供 JSON 参数编辑器做实时错误标记。
 *
 * 平台的 JSON 解析器非常宽松：缺值、未闭合括号这类结构错误会产出
 * [com.intellij.psi.PsiErrorElement]，但尾逗号、未加引号的键、未闭合字符串
 * 都当作合法输入。这里用提交参数时同一个 Gson 解析库的严格模式补齐校验：
 * 注释先抹成等长空白，报错位置与编辑器文本一一对应。
 */
object DebugJsonSyntax {

    /** 错误消息与出错位置（指向出错字符的偏移）。 */
    data class Issue(val message: String, val offset: Int)

    private val LOCATION = Regex("line (\\d+) column (\\d+)")

    /** 严格校验；文本合法（允许注释）时返回 null。 */
    fun strictIssue(text: String): Issue? {
        val blanked = blankComments(text)
        if (blanked.isBlank()) {
            return null
        }
        val reader = JsonReader(StringReader(blanked))
        reader.isLenient = false
        return try {
            JsonParser.parseReader(reader)
            // 顶层值解析完后不允许再出现任何内容
            if (reader.peek() == JsonToken.END_DOCUMENT) {
                null
            } else {
                Issue("JSON 结尾有多余内容", offsetOf(blanked, reader))
            }
        } catch (e: Exception) {
            Issue(messageOf(e), offsetOf(blanked, reader))
        }
    }

    /** 把注释替换成等长空白，字符串内的斜杠不动，偏移保持不变。 */
    private fun blankComments(text: String): String {
        val out = StringBuilder(text)
        var index = 0
        var inString = false
        var escaped = false
        while (index < out.length) {
            val c = out[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                index++
                continue
            }
            when {
                c == '"' -> {
                    inString = true
                    index++
                }
                c == '/' && index + 1 < out.length && out[index + 1] == '/' -> {
                    while (index < out.length && out[index] != '\n') {
                        out.setCharAt(index++, ' ')
                    }
                }
                c == '/' && index + 1 < out.length && out[index + 1] == '*' -> {
                    out.setCharAt(index++, ' ')
                    out.setCharAt(index++, ' ')
                    while (index + 1 < out.length && !(out[index] == '*' && out[index + 1] == '/')) {
                        if (out[index] != '\n') {
                            out.setCharAt(index, ' ')
                        }
                        index++
                    }
                    if (index + 1 < out.length) {
                        out.setCharAt(index++, ' ')
                        out.setCharAt(index++, ' ')
                    }
                }
                else -> index++
            }
        }
        return out.toString()
    }

    /** 从 Gson 的 reader 位置还原字符偏移；拿不到位置时退回文本开头。 */
    private fun offsetOf(text: String, reader: JsonReader): Int {
        val match = LOCATION.find(reader.toString()) ?: return 0
        var line = match.groupValues[1].toInt()
        val column = match.groupValues[2].toInt()
        var offset = 0
        while (line > 1 && offset < text.length) {
            if (text[offset] == '\n') {
                line--
            }
            offset++
        }
        if (offset >= text.length) {
            return (text.length - 1).coerceAtLeast(0)
        }
        return (offset + column - 1).coerceIn(offset, text.length - 1)
    }

    /** 去掉 Gson 消息里的行列尾巴，只留人能看懂的部分。 */
    private fun messageOf(error: Exception): String {
        val raw = (error as? JsonSyntaxException)?.cause?.message
            ?: error.message
            ?: return "JSON 语法错误"
        if (raw.contains("EOFException")) {
            return "JSON 不完整：缺少结尾"
        }
        return raw.substringBefore(" at line ").ifBlank { "JSON 语法错误" }
    }
}
