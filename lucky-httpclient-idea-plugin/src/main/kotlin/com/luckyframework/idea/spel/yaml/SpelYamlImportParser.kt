package com.luckyframework.idea.spel.yaml

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.metadata.SpelLifecycle
import com.luckyframework.idea.spel.symbol.ImportedClassSymbols
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLValue

/**
 * 解析 YAML 中的 `SpELImportConf` 配置节点（design D10）。
 *
 * 识别 `springElImport` 下的 `root`/`val`/`rootLit`/`varLit`/`classes`/
 * `pack`/`running`，键名同时兼容 camelCase 与 kebab-case。作用范围仅限
 * 当前文件，保证不同配置文件之间不泄漏符号。
 */
class SpelYamlImportParser(
    private val importedClasses: ImportedClassSymbols = ImportedClassSymbols()
) {

    fun symbols(file: PsiFile): List<SpelSymbol> {
        val result = ArrayList<SpelSymbol>()
        for (block in importBlocks(file)) {
            for (entry in block.keyValues) {
                when (normalize(entry.keyText)) {
                    ROOT, ROOT_LIT -> result += variableSymbols(entry, root = true)
                    VAL, VAR_LIT -> result += variableSymbols(entry, root = false)
                    CLASSES -> result += classSymbols(file, entry.value)
                    else -> Unit
                }
            }
        }
        return result
    }

    fun packages(file: PsiFile): List<String> =
        importBlocks(file)
            .flatMap { block ->
                block.keyValues
                    .filter { normalize(it.keyText) == PACK }
                    .flatMap { scalarValues(it.value) }
            }
            .distinct()

    private fun importBlocks(file: PsiFile): List<YAMLMapping> =
        PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .filter { normalize(it.keyText) == SPRING_EL_IMPORT }
            .mapNotNull { it.value as? YAMLMapping }

    /** `root`/`val`/`rootLit`/`varLit` 的值是「变量名 -> 值」映射，子键才是变量。 */
    private fun variableSymbols(container: YAMLKeyValue, root: Boolean): List<SpelSymbol> {
        val mapping = container.value as? YAMLMapping ?: return emptyList()
        return mapping.keyValues.mapNotNull { entry ->
            val name = nameOf(entry)
            // 动态变量名（含 SpEL 表达式）无法静态确定，跳过
            if (name.isEmpty() || name.contains("#{")) {
                return@mapNotNull null
            }
            SpelSymbol(
                name = name,
                kind = if (root) SpelSymbolKind.ROOT_VARIABLE else SpelSymbolKind.VARIABLE,
                type = VALUE_TYPE,
                origin = SpelSymbolOrigin.CLASS_IMPORT,
                declaration = entry,
                lifecycle = SpelLifecycle.REQUEST_INIT,
                sourceRange = nameRange(entry),
                description = "YAML springElImport"
            )
        }
    }

    private fun classSymbols(file: PsiFile, value: YAMLValue?): List<SpelSymbol> {
        if (value == null) {
            return emptyList()
        }
        val facade = JavaPsiFacade.getInstance(file.project)
        val scope = GlobalSearchScope.allScope(file.project)
        return scalarValues(value)
            .mapNotNull { facade.findClass(classNameOf(it), scope) }
            .flatMap { importedClasses.symbolsOf(it, SpelSymbolOrigin.CLASS_IMPORT) }
    }

    private fun classNameOf(raw: String): String {
        var name = raw.trim()
        if (name.startsWith("T(") && name.endsWith(")")) {
            name = name.substring(2, name.length - 1)
        }
        if (name.startsWith("#{") && name.endsWith("}")) {
            name = name.substring(2, name.length - 1)
        }
        return name.trim()
    }

    private fun scalarValues(value: YAMLValue?): List<String> = when (value) {
        is YAMLSequence -> value.items.mapNotNull { (it.value as? YAMLScalar)?.textValue }
        is YAMLScalar -> listOf(value.textValue)
        else -> emptyList()
    }

    private fun nameOf(entry: YAMLKeyValue): String {
        val raw = (entry.key as? YAMLScalar)?.textValue ?: entry.keyText
        return raw.trim().removeSurrounding("\"").removeSurrounding("'")
    }

    private fun nameRange(entry: YAMLKeyValue): TextRange? {
        val key = entry.key ?: return null
        val shift = key.textRange.startOffset - entry.textRange.startOffset
        if (shift < 0) {
            return null
        }
        return key.textRange.shiftLeft(entry.textRange.startOffset)
    }

    /** camelCase、kebab-case 与 snake_case 统一成小写去分隔符的形式。 */
    private fun normalize(key: String): String =
        key.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .lowercase()
            .replace("-", "")
            .replace("_", "")

    private companion object {
        const val SPRING_EL_IMPORT = "springelimport"
        const val ROOT = "root"
        const val VAL = "val"
        const val ROOT_LIT = "rootlit"
        const val VAR_LIT = "varlit"
        const val CLASSES = "classes"
        const val PACK = "pack"
        const val VALUE_TYPE = "Object"
    }
}
