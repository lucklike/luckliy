package com.luckyframework.idea.spel.component

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import javax.swing.Icon

/**
 * `@LuckyComponent` 组件类在导航、结构视图、查找类等位置显示品牌图标。
 *
 * 组件身份来自 [SpelRuntimeSymbols] 的注解扫描，与补全/检查同源；快照
 * 尚未预热或 dumb 模式下返回 null，不阻塞索引构建。
 *
 * 图标本身由 [ComponentIconResolver] 按设置解析：可在设置页关闭替换或指定
 * 自定义图标文件，未配置时使用插件内置图标。
 */
class LuckyComponentIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        val psiClass = element as? PsiClass ?: return null
        return if (isLuckyComponentClass(psiClass)) ComponentIconResolver.icon() else null
    }
}

/**
 * 类是否被声明为 `@LuckyComponent` 组件。
 *
 * 组件身份统一来自 [SpelRuntimeSymbols] 的注解扫描，PSI 级图标
 * （项目树、结构视图）与文件级图标（编辑器标签页）共用同一判定，
 * 保证同一组件在不同位置的图标一致。
 *
 * 这里只读 [SpelRuntimeSymbols.cachedComponents]：图标解析可能在 EDT 上
 * 发生（例如项目树、结构视图的重绘），主动触发注解索引查询会阻塞 UI，
 * 内部模式下还会直接断言失败。判定使用类简单名，不调用
 * `getQualifiedName()`：2026.3 的绘制路径没有读锁，后者会沿 stub 链解析
 * 父级并抛 `Read access is allowed from inside read-action only`。预热由
 * [LuckyComponentIconRefresher] 在后台完成，预热后的下一次重绘自然命中
 * 新清单。
 */
internal fun isLuckyComponentClass(psiClass: PsiClass): Boolean {
    val name = runCatching { psiClass.name }.getOrNull() ?: return false
    return ComponentNameCache.names(psiClass.project).matchesSimpleName(name)
}
