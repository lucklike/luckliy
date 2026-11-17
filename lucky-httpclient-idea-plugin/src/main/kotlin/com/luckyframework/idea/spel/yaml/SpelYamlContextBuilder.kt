package com.luckyframework.idea.spel.yaml

import com.luckyframework.idea.spel.metadata.SpelLifecycle
import com.luckyframework.idea.spel.symbol.ProjectSpelSymbolSource
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * 从 YAML 标量构建 SpEL 上下文。
 *
 * 符号来源只有两类：当前文件 `springElImport` 中的声明，以及运行时从
 * 项目依赖扫描到的框架符号。作用域按文件隔离，不读取其他 YAML 文件或
 * Java 注解。
 */
class SpelYamlContextBuilder(
    private val importParser: SpelYamlImportParser = SpelYamlImportParser(),
    private val runtimeSource: ProjectSpelSymbolSource = SpelRuntimeSymbols
) {

    fun build(scalar: YAMLScalar): SpelContext {
        val file = scalar.containingFile ?: return SpelContext.unknown()
        val runtime = runtimeSource.symbols(file.project)
        return SpelContext(
            known = true,
            stage = SpelLifecycle.REQUEST_INIT,
            attribute = null,
            symbols = merge(importParser.symbols(file), runtime)
        )
    }

    /** 配置声明优先于运行时符号；函数按签名保留同名重载。 */
    private fun merge(config: List<SpelSymbol>, runtime: List<SpelSymbol>): List<SpelSymbol> {
        val seen = HashSet<Triple<SpelSymbolKind, String?, String?>>()
        val result = ArrayList<SpelSymbol>()
        for (symbol in config + runtime) {
            val key = if (symbol.kind == SpelSymbolKind.FUNCTION) {
                Triple(symbol.kind, symbol.namespace, symbol.signature ?: symbol.name)
            } else {
                Triple(symbol.kind, symbol.namespace, symbol.name)
            }
            if (seen.add(key)) {
                result += symbol
            }
        }
        return result
    }
}
