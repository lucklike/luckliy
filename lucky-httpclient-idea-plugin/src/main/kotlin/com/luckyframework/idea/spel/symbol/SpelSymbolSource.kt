package com.luckyframework.idea.spel.symbol

/**
 * 符号层来源：内置符号表、全局变量配置等。
 *
 * 第 6 组只定义接口；内置符号表由第 7 组从框架源码生成后接入。
 */
interface SpelSymbolSource {
    fun symbols(): List<SpelSymbol>
}

object EmptySpelSymbolSource : SpelSymbolSource {
    override fun symbols(): List<SpelSymbol> = emptyList()
}
