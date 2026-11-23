package com.luckyframework.idea.debug.invoke

/**
 * JSON 参数文本的格式化。
 *
 * 参数模板带 `//` 注释，交给 Gson 解析会直接失败；这里按 JSON 词法重排
 * 空白，不依赖解析器：对象/数组每层缩进两格、逗号后换行、冒号后补一个空格，
 * 注释保留原文并独占一行。输入不完整时也尽量输出，不抛异常打断编辑。
 */
internal object DebugJsonFormatter {

    fun format(text: String, indent: String = "  "): String {
        val tokens = tokenize(text)
        val out = StringBuilder(text.length)
        var depth = 0
        var lineStart = true
        var spaceBeforeNext = false

        fun newline() {
            if (out.isEmpty() || out.last() != '\n') {
                out.append('\n')
            }
            lineStart = true
            spaceBeforeNext = false
        }

        fun write(token: String) {
            if (lineStart) {
                repeat(depth) { out.append(indent) }
                lineStart = false
            } else if (spaceBeforeNext) {
                out.append(' ')
            }
            spaceBeforeNext = false
            out.append(token)
        }

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            when (token.kind) {
                TokenKind.OPEN -> {
                    val next = tokens.getOrNull(index + 1)
                    val empty = next != null && next.kind == TokenKind.CLOSE && closes(token.text, next.text)
                    write(token.text)
                    if (empty) {
                        // 空对象/空数组保持一行，避免格式化后多出两行
                        out.append(next!!.text)
                        index++
                    } else {
                        depth++
                        newline()
                    }
                }

                TokenKind.CLOSE -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    newline()
                    write(token.text)
                }

                TokenKind.COMMA -> {
                    out.append(',')
                    newline()
                }

                TokenKind.COLON -> {
                    write(":")
                    spaceBeforeNext = true
                }

                TokenKind.VALUE -> write(token.text)

                TokenKind.COMMENT -> {
                    if (token.text.startsWith("//")) {
                        // 行注释独占一行：跟在值后面会把右侧的值推到下一行，读起来更乱
                        if (!lineStart) {
                            newline()
                        }
                        write(token.text)
                        newline()
                    } else {
                        // 块注释常插在值与逗号之间，保持同行并补空格
                        if (!lineStart) {
                            out.append(' ')
                        }
                        write(token.text)
                        spaceBeforeNext = true
                    }
                }
            }
            index++
        }
        return out.toString().trimEnd()
    }

    private fun closes(open: String, close: String): Boolean =
        (open == "{" && close == "}") || (open == "[" && close == "]")

    private fun tokenize(text: String): List<Token> {
        val tokens = ArrayList<Token>()
        var index = 0
        while (index < text.length) {
            val current = text[index]
            when {
                current.isWhitespace() -> index++

                current == '"' -> {
                    val start = index
                    index++
                    while (index < text.length) {
                        val ch = text[index]
                        if (ch == '\\') {
                            index += 2
                            continue
                        }
                        index++
                        if (ch == '"') {
                            break
                        }
                    }
                    val end = index.coerceAtMost(text.length)
                    index = end
                    tokens.add(Token(TokenKind.VALUE, text.substring(start, end)))
                }

                current == '/' && text.getOrNull(index + 1) == '/' -> {
                    val start = index
                    while (index < text.length && text[index] != '\n') {
                        index++
                    }
                    tokens.add(Token(TokenKind.COMMENT, text.substring(start, index).trimEnd()))
                }

                current == '/' && text.getOrNull(index + 1) == '*' -> {
                    val start = index
                    val close = text.indexOf("*/", index + 2)
                    index = if (close < 0) text.length else close + 2
                    tokens.add(Token(TokenKind.COMMENT, text.substring(start, index)))
                }

                current == '{' || current == '[' -> {
                    tokens.add(Token(TokenKind.OPEN, current.toString()))
                    index++
                }

                current == '}' || current == ']' -> {
                    tokens.add(Token(TokenKind.CLOSE, current.toString()))
                    index++
                }

                current == ',' -> {
                    tokens.add(Token(TokenKind.COMMA, ","))
                    index++
                }

                current == ':' -> {
                    tokens.add(Token(TokenKind.COLON, ":"))
                    index++
                }

                else -> {
                    val start = index
                    while (index < text.length && !isDelimiter(text, index)) {
                        index++
                    }
                    if (index == start) {
                        // 未识别的字符原样保留，避免格式化丢内容
                        index++
                    }
                    tokens.add(Token(TokenKind.VALUE, text.substring(start, index)))
                }
            }
        }
        return tokens
    }

    private fun isDelimiter(text: String, index: Int): Boolean {
        val ch = text[index]
        if (ch.isWhitespace() || ch == '{' || ch == '}' || ch == '[' || ch == ']' ||
            ch == ',' || ch == ':' || ch == '"'
        ) {
            return true
        }
        return ch == '/' && (text.getOrNull(index + 1) == '/' || text.getOrNull(index + 1) == '*')
    }

    private class Token(val kind: TokenKind, val text: String)

    private enum class TokenKind { OPEN, CLOSE, COMMA, COLON, VALUE, COMMENT }
}
