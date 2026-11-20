package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.luckyframework.idea.spel.metadata.LuckyComponentMetadata
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import java.util.Collections
import java.util.WeakHashMap

/**
 * 组件名称索引：补全渲染、编辑器标签页图标等高频路径用它判定组件身份。
 *
 * 这些路径都可能在 EDT 的绘制阶段执行；2026.3 起绘制调用不再持有隐式读锁
 * （`RwLockHolder.runWithoutImplicitRead`），此时访问 `PsiClass.getQualifiedName()`
 * 会沿 stub 链解析父级并抛 `Read access is allowed from inside read-action only`。
 * 因此判定只使用**不触发 PSI 解析的缓存数据**：
 *
 * - 类名补全候选取 `JavaPsiClassReferenceElement` 构造时缓存的 `myQualifiedName`
 *   （`getQualifiedName()` 只读字段，2024.1 与 2026.3 一致）；
 * - 其他形态的候选、文件标签页、结构视图按名称文本匹配。
 */
internal class ComponentNames(
    private val qualifiedNames: Set<String>,
    private val simpleNames: Set<String>
) {

    val isEmpty: Boolean get() = qualifiedNames.isEmpty()

    /** 补全候选是否组件类。 */
    fun matches(item: LookupElement): Boolean {
        if (item.`object` !is PsiClass) {
            return false
        }
        val qualifiedName = cachedQualifiedName(item)
        if (qualifiedName != null) {
            return qualifiedName in qualifiedNames
        }
        // 非类名候选（例如被其他元素包装或来自其他语言）只能按插入文本匹配，
        // 简单名与全限定名都接受，仍然不访问 PSI
        val text = runCatching { item.lookupString }.getOrNull() ?: return false
        return text in qualifiedNames || text.substringAfterLast('.') in simpleNames
    }

    /** 简单名（类名、Java 文件名）是否组件。 */
    fun matchesSimpleName(name: String): Boolean = name in simpleNames

    /** 兼容补全项被 `LookupElementDecorator` 包裹的情况。 */
    private fun cachedQualifiedName(item: LookupElement): String? {
        var current: LookupElement? = item
        while (current != null) {
            val element = current
            if (element is JavaPsiClassReferenceElement) {
                return runCatching { element.qualifiedName }.getOrNull()
            }
            current = (element as? LookupElementDecorator<*>)?.delegate
        }
        return null
    }

    companion object {
        val EMPTY = ComponentNames(emptySet(), emptySet())

        fun of(components: List<LuckyComponentMetadata>): ComponentNames {
            val qualified = HashSet<String>(components.size * 2)
            val simple = HashSet<String>(components.size * 2)
            for (component in components) {
                qualified.add(component.className)
                simple.add(component.className.substringAfterLast('.'))
            }
            return ComponentNames(qualified, simple)
        }
    }
}

/**
 * 组件名称索引的项目级缓存，随最近一次扫描清单的引用失效。
 *
 * 清单只在后台读操作里更新（[SpelRuntimeSymbols.cachedComponents]），这里
 * 只做引用比较，既不访问 PSI 也不触发索引查询，可以在绘制路径上调用。
 */
internal object ComponentNameCache {

    private val cache = Collections.synchronizedMap(WeakHashMap<Project, Entry>())

    private class Entry(
        val source: List<LuckyComponentMetadata>,
        val names: ComponentNames
    )

    fun names(project: Project): ComponentNames {
        val components = SpelRuntimeSymbols.cachedComponents(project) ?: return ComponentNames.EMPTY
        cache[project]?.let {
            if (it.source === components) {
                return it.names
            }
        }
        val names = ComponentNames.of(components)
        cache[project] = Entry(components, names)
        return names
    }
}
