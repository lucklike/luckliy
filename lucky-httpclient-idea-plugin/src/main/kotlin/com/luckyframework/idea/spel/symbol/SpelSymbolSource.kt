package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.project.Project

/**
 * 符号层来源：测试注入的附加符号、全局变量配置等。
 */
interface SpelSymbolSource {
    fun symbols(): List<SpelSymbol>
}

object EmptySpelSymbolSource : SpelSymbolSource {
    override fun symbols(): List<SpelSymbol> = emptyList()
}

/**
 * 按项目解析的符号来源：运行时扫描框架类得到的变量与函数。
 *
 * 与 [SpelSymbolSource] 的区别是符号随项目 PSI 变化，不能做成单例静态表。
 */
fun interface ProjectSpelSymbolSource {
    fun symbols(project: Project): List<SpelSymbol>

    companion object {
        /** 不提供任何符号；测试隔离运行时扫描时使用。 */
        val EMPTY = ProjectSpelSymbolSource { emptyList() }
    }
}
