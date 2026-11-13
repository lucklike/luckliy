package com.luckyframework.idea.spel.completion

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.luckyframework.idea.spel.lexer.SpelElementScope

/**
 * 选择/投影方括号内的元素上下文。
 *
 * 由外到内逐层解析作用域的接收者，得到最内层方括号的当前元素类型：
 * `groups.?[x].![y]` 的投影以 `Group` 为元素，`tags.?[...]` 再以
 * `tags` 的元素为当前元素。任何一层无法推导时返回 null，调用方按
 * 「元素类型未知」降级，不产生误报。
 */
internal object SpelElementContext {

    /** 最内层方括号的元素类型；不在元素上下文中或任一层无法推导时返回 null。 */
    fun elementType(scopes: List<SpelElementScope>, resolver: SpelTypeResolver): PsiType? {
        var element: PsiType? = null
        for (scope in scopes) {
            val container = if (scope.base.isEmpty()) {
                element
            } else {
                resolver.resolvePsiType(scope.base, element)
            }
            element = resolver.elementTypeOf(container) ?: return null
        }
        return element
    }

    /** 元素类型对应的类；元素类型未知时返回 null。 */
    fun elementClass(scopes: List<SpelElementScope>, resolver: SpelTypeResolver): PsiClass? =
        resolver.classOf(elementType(scopes, resolver))
}
