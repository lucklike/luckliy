package com.luckyframework.idea.spel.yaml

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import com.luckyframework.idea.spel.metadata.SpelConfigField
import com.luckyframework.idea.spel.metadata.SpelConfigIndex
import com.luckyframework.idea.spel.metadata.SpelConfigPathMatch
import com.luckyframework.idea.spel.metadata.SpelConfigPathResolver
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
 *
 * 标量所在键路径会映射到 `@SpelConfig` 配置模型（[SpelYamlConfigPath] +
 * [SpelConfigPathResolver]）：
 *
 * - 命中字段的 `kind` 不在表达式白名单内（`NONE`、`DATA`、`STRUCT` 等）时
 *   返回未知上下文，与 Java 注解路径的判定一致，补全/引用/检查全部跳过；
 * - 命中字段的 `resultType`/`valueType` 作为 [SpelContext.expectedType]，
 *   供类型推导判断表达式结果与配置字段是否匹配；
 * - 路径未命中任何配置模型时保持原有行为（YAML 中的 `#{}` 一律按表达式处理），
 *   这样用户自定义命名空间的配置不会失去补全。
 */
class SpelYamlContextBuilder(
    private val importParser: SpelYamlImportParser = SpelYamlImportParser(),
    private val runtimeSource: ProjectSpelSymbolSource = SpelRuntimeSymbols
) {

    fun build(scalar: YAMLScalar): SpelContext {
        val file = scalar.containingFile ?: return SpelContext.unknown()
        val match = matchOf(scalar)
        if (match != null && skipsExpressions(match)) {
            return SpelContext.unknown()
        }
        val runtime = runtimeSource.symbols(file.project)
        return SpelContext(
            known = true,
            stage = SpelLifecycle.REQUEST_INIT,
            attribute = null,
            symbols = merge(importParser.symbols(file), runtime),
            expectedType = match?.field?.expectedType?.let { typeOf(file.project, it) }
        )
    }

    /** 标量命中到的配置字段；未命中或路径不在配置模型中时返回 null。 */
    fun configFieldOf(scalar: YAMLScalar): SpelConfigField? = matchOf(scalar)?.field

    /**
     * 标量所在配置字段是否允许表达式。
     *
     * 未命中配置模型时返回 `true`（保持 YAML 中的原有行为）；命中但
     * `kind` 不在白名单内时返回 `false`。高亮等只需要「是否解析」的
     * 调用方用它，避免为此构建整个符号表。
     */
    fun acceptsExpressions(scalar: YAMLScalar): Boolean {
        val match = matchOf(scalar) ?: return true
        return !skipsExpressions(match)
    }

    private fun matchOf(scalar: YAMLScalar): SpelConfigPathMatch? {
        val project = scalar.project
        if (DumbService.isDumb(project)) {
            return null
        }
        val path = SpelYamlConfigPath.of(scalar)
        if (path.isEmpty()) {
            return null
        }
        val models = SpelConfigIndex.getInstance(project).models()
        if (models.isEmpty()) {
            return null
        }
        return SpelConfigPathResolver(models).resolve(path)
    }

    /**
     * 命中字段是否不允许表达式。
     *
     * 只有能确定值归属时才跳过：字段被精确命中，或字段本身是 Map
     * （剩余段是实例键，条目值仍属于该字段）。字段之后还有未知层级时
     * （如 `STRUCT` 字段内部没有对应配置模型），不擅自判定。
     */
    private fun skipsExpressions(match: SpelConfigPathMatch): Boolean =
        !match.field.expressive && (match.remaining.isEmpty() || match.field.map)

    /** 配置字段的类型名 → [PsiType]；解析不到时返回 null，不做任何判定。 */
    private fun typeOf(project: Project, typeName: String): PsiType? {
        val name = typeName.trim()
        if (name.isEmpty()) {
            return null
        }
        PRIMITIVE_TYPES[name]?.let { return it }
        val facade = JavaPsiFacade.getInstance(project)
        facade.findClass(name, GlobalSearchScope.allScope(project))?.let {
            return facade.elementFactory.createType(it)
        }
        return runCatching { facade.elementFactory.createTypeFromText(name, null) }.getOrNull()
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

    private companion object {
        /** `resultType`/`valueType` 存的是类型名，基本类型没有 PSI 类可查。 */
        val PRIMITIVE_TYPES = mapOf(
            "boolean" to PsiTypes.booleanType(),
            "byte" to PsiTypes.byteType(),
            "char" to PsiTypes.charType(),
            "short" to PsiTypes.shortType(),
            "int" to PsiTypes.intType(),
            "long" to PsiTypes.longType(),
            "float" to PsiTypes.floatType(),
            "double" to PsiTypes.doubleType()
        )
    }
}
