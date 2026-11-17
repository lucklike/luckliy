package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypeElement
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 7.2 内置符号表接入符号解析。
 */
class BuiltinSymbolTest : SymbolFixtureTestCase() {

    /** 变量完全由项目内的 `@SpelSymbol` 字段常量发现。 */
    fun testRuntimeDiscoversVariables() {
        val symbols = SpelRuntimeSymbols.variables(project)

        val status = symbols.first { it.name == "\$status\$" }
        assertEquals(SpelSymbolKind.ROOT_VARIABLE, status.kind)
        assertEquals("Integer", status.type)
        assertEquals(SpelLifecycle.RESPONSE_INIT, status.lifecycle)
        assertEquals(SpelSymbolOrigin.BUILTIN, status.origin)
        assertEquals("响应状态码", status.description)

        val body = symbols.first { it.name == "\$body\$" }
        assertEquals(SpelSymbolKind.ROOT_VARIABLE, body.kind)
        assertEquals(SpelLifecycle.RESPONSE_INIT, body.lifecycle)

        // `kind = FUNCTION` 的常量只描述函数名，不参与变量补全
        assertTrue(symbols.none { it.name == "parameterInstance" })
    }

    /**
     * 变量类型来自 `@SpelSymbol` 的 `type` / `typeExpression` 声明。
     */
    fun testVariableTypesMatchRuntimeValues() {
        val types = SpelRuntimeSymbols.variables(project).associate { it.name to it.type }

        assertEquals("ApiDescribe", types["\$api\$"])
        assertEquals("ResolvableType", types["\$mrt\$"])
        assertEquals("ResolvableType", types["\$mcrt\$"])
        assertEquals("ResolvableType[]", types["\$mpts\$"])
        assertEquals("ResolvableType", types["_type_"])
        assertEquals("ContentType", types["\$contentType\$"])
        assertEquals("Map<String, Object>", types["\$query\$"])
        assertEquals("Map<String, Object>", types["\$path\$"])
        assertEquals("Map<String, Object>", types["\$form\$"])
        assertEquals("Map<String, Object>", types["\$reqHeader\$"])
        assertEquals("Map<String, Object>", types["\$reqCookie\$"])
        assertEquals("Map<String, Object>", types["\$respHeader\$"])
        assertEquals("Map<String, Object>", types["\$respCookie\$"])
        assertEquals("String", types["__\$asyncExecutor\$__"])
        assertEquals("String", types["__\$asyncConcurrency\$__"])
        assertEquals("Model", types["__\$asyncModel\$__"])
        assertEquals("HttpExecutor", types["__\$httpExecutor\$__"])
        assertEquals("MockResponseFactory", types["__\$mockResponseFactory\$__"])
        assertEquals("StandardEventListener", types["__\$eventListener\$__"])
        assertEquals("Function", types["__\$retryRunBeforeRetryContextFunction\$__"])
        assertEquals("Function", types["__\$retryRetryDeciderContextFunction\$__"])
    }

    /** `$this$` 按所在类推导类型，`$result$` 按所在方法返回类型推导。 */
    fun testThisAndResultTypesFollowDeclaration() {
        // 测试工程没有挂载 JDK，补源码桩让返回类型可解析
        myFixture.addFileToProject(
            "java/lang/String.java",
            "package java.lang;\npublic class String {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{x}")
    String fetch() {
        return "";
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{x}"))

        val thisSymbol = context.find("\$this\$", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals("Api", thisSymbol.type)
        assertTrue("`\$this\$` 应声明到所在类", thisSymbol.declaration is PsiClass)

        val resultSymbol = context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals("String", resultSymbol.type)
        assertEquals("fetch", (resultSymbol.declaration as PsiMethod).name)
        // 跳转优先到返回类型中类型的书写位置，而不是方法或类声明
        assertEquals("String", (resultSymbol.jumpTarget as PsiTypeElement).text)
    }

    /** `$result$` 对齐 `getResultResolvableType()`：Optional/Future 等包装类型拆包。 */
    fun testResultTypeUnwrapsPackTypes() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import java.util.Optional;

class Api {
    @HttpRequest(url = "#{opt}")
    Optional<Item> optional() {
        return Optional.empty();
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{opt}"))

        val result = context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals("Item", result.type)
        assertNotNull("应保留推导出的 PSI 类型", result.resolvedType)
        assertEquals("optional", (result.declaration as PsiMethod).name)
        // 拆包后的 `Item` 在返回类型 `Optional<Item>` 中的书写位置才是跳转目标
        assertEquals("Item", (result.jumpTarget as PsiTypeElement).text)
    }

    /** void 方法 + `ResultHandler<T>` 参数：结果类型取处理器泛型。 */
    fun testResultTypeFromResultHandlerParameter() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

class Api {
    @HttpRequest(url = "#{handler}")
    void fetch(ResultHandler<Item> handler) {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        val result = context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals("Item", result.type)
    }

    /** 自定义注解按同名属性把 handlerClass 传给 `@ResultHandlerMeta`（隐式别名）。 */
    fun testResultTypeFromCustomResultHandlerMeta() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

@ResultHandlerMeta
@interface MyHandlerMeta {
    Class<? extends ResultHandler> handlerClass() default ResultHandler.class;
}

class Api {
    @HttpRequest(url = "#{handler}")
    @MyHandlerMeta(handlerClass = ItemHandler.class)
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Item", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 自定义注解用显式 `@AliasFor` 指向 `@ResultHandlerMeta#handlerClass`。 */
    fun testResultTypeFromAliasedResultHandlerMeta() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;
import org.springframework.core.annotation.AliasFor;

@ResultHandlerMeta
@interface MyHandlerMeta {
    @AliasFor(annotation = ResultHandlerMeta.class, attribute = "handlerClass")
    Class<? extends ResultHandler> customClass() default ResultHandler.class;
}

class Api {
    @HttpRequest(url = "#{handler}")
    @MyHandlerMeta(customClass = ItemHandler.class)
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Item", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 对齐 `generateObject(handler(), handlerClass(), ...)`：`handler()` 优先。 */
    fun testResultTypePrefersHandlerAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

class Api {
    @HttpRequest(url = "#{handler}")
    @ResultHandlerMeta(
        handlerClass = IgnoredHandler.class,
        handler = @ObjectGenerate(ItemHandler.class)
    )
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}

class IgnoredHandler implements ResultHandler<Integer> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Item", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 自定义注解声明处的 `@ResultHandlerMeta` 默认值同样生效。 */
    fun testResultTypeFromResultHandlerMetaDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

@ResultHandlerMeta(handlerClass = ItemHandler.class)
@interface MyHandlerMeta {
}

class Api {
    @HttpRequest(url = "#{handler}")
    @MyHandlerMeta
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Item", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** `handler()` 只配置了 msg 时运行期类型不可知，`$result$` 保持内置的 Object。 */
    fun testResultTypeStaysObjectForUnresolvableHandler() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;

class Api {
    @HttpRequest(url = "#{handler}")
    @ResultHandlerMeta(handler = @ObjectGenerate(msg = "handlerBean"))
    void fetch() {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Object", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 显式别名先指向自定义注解的元注解属性，再由隐式别名接到 `@ResultHandlerMeta#handlerClass`。 */
    fun testResultTypeFromAliasForToMetaAttribute() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;
import org.springframework.core.annotation.AliasFor;

@ResultHandlerMeta
@interface MyHandlerMeta {
    Class<? extends ResultHandler> handlerClass() default ResultHandler.class;
}

@MyHandlerMeta
@interface MyAliasHandlerMeta {

    @AliasFor(annotation = MyHandlerMeta.class, attribute = "handlerClass")
    Class<? extends ResultHandler> type() default ResultHandler.class;
}

class Api {
    @HttpRequest(url = "#{handler}")
    @MyAliasHandlerMeta(type = ItemHandler.class)
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Item", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** `@ResultHandlerMeta` 带 `@Inherited`，方法所在类的父类上声明时同样生效。 */
    fun testResultTypeFromResultHandlerMetaOnSuperClass() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

@ResultHandlerMeta(handlerClass = ItemHandler.class)
class BaseApi {
}

class Api extends BaseApi {

    @HttpRequest(url = "#{handler}")
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{handler}"))

        assertEquals("Item", context.find("\$result\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** `$body$`/`$jsonBody$`/`$xmlBody$` 对齐 `getConvertMetaType().getMetaType()`。 */
    fun testBodyTypesFollowConvertMetaType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body}")
    @ConvertMetaType(Item.class)
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))

        for (name in listOf("\$body\$", "\$jsonBody\$", "\$xmlBody\$")) {
            val symbol = context.find(name, SpelSymbolKind.ROOT_VARIABLE).single()
            assertEquals("$name 应使用 metaType", "Item", symbol.type)
            assertNotNull("$name 应保留推导出的 PSI 类型", symbol.resolvedType)
            assertEquals(
                "$name 应将注解中的类字面量作为跳转目标",
                "Item.class",
                (symbol.jumpTarget as PsiClassObjectAccessExpression).text
            )
        }
        // 与 metaType 无关的响应体保持内置类型
        assertEquals("Object", context.find("\$javaBody\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 类上的 `@ConvertMetaType` 与方法同效，方法未标注时使用类声明。 */
    fun testBodyTypeFromClassConvertMetaType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

@ConvertMetaType(Item.class)
class Api {
    @HttpRequest(url = "#{body}")
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** `@ResultConvertMeta(metaType = X.class)` 经 `@AliasFor` 合并到 `@ConvertMetaType#value`。 */
    fun testBodyTypeFromResultConvertMetaAlias() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;

class Api {
    @HttpRequest(url = "#{body}")
    @ResultConvertMeta(metaType = Item.class)
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /**
     * `@RespConvert(metaType = X.class)` 经两级 `@AliasFor` 传递到 `@ConvertMetaType#value`：
     * `RespConvert#metaType` → `ResultConvertMeta#metaType` → `ConvertMetaType#value`。
     * 表达式写在 `value`（`result` 的注解内别名）里，阶段为 RESPONSE，`$body$` 可见。
     */
    fun testBodyTypeFromRespConvertMetaType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.RespConvert;

class Api {
    @RespConvert(value = "#{body}", metaType = Item.class)
    void fetch() {
    }
}

class Item {
    public String name;
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))

        val body = context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals("Item", body.type)
        assertEquals(SpelVisibility.AVAILABLE, body.visibility)
    }

    /** 类上的 `@RespConvert(metaType = X.class)` 与方法同效，且不需要显式写表达式属性。 */
    fun testBodyTypeFromClassRespConvertMetaType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;

@RespConvert(metaType = Item.class)
class Api {
    @HttpRequest(url = "#{body}")
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /**
     * 组合注解声明上的 `@ConvertMetaType` 只会提供默认值（`Object`），
     * 不能按注解顺序覆盖用户显式写的 `metaType`。
     */
    fun testBodyTypePrefersExplicitMetaTypeOverMetaDefault() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.convert.ResultSelectionResponseConvert;

class Api {
    @HttpRequest(url = "#{body}")
    @ResultConvertMeta(convert = @ObjectGenerate(ResultSelectionResponseConvert.class))
    @RespConvert(metaType = Item.class)
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /**
     * 用户基于框架元注解扩展自定义注解时，只要属性同名，运行期 Spring 的隐式别名
     * 就会把值传递到 `@ConvertMetaType`，插件也必须能推导出 `$body$` 的类型。
     */
    fun testBodyTypeFromCustomCompositeAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.convert.ResultSelectionResponseConvert;
import com.luckyframework.reflect.Combination;

@Combination(ResultConvertMeta.class)
@ResultConvertMeta(convert = @ObjectGenerate(ResultSelectionResponseConvert.class))
@interface MyConvert {
    Class<?> metaType() default Object.class;
}

class Api {
    @HttpRequest(url = "#{body}")
    @MyConvert(metaType = Item.class)
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 自定义组合注解声明处的元注解显式值也应作为默认值参与推导。 */
    fun testBodyTypeFromCustomCompositeAnnotationDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.convert.ResultSelectionResponseConvert;
import com.luckyframework.reflect.Combination;

@Combination(ResultConvertMeta.class)
@ResultConvertMeta(metaType = Item.class, convert = @ObjectGenerate(ResultSelectionResponseConvert.class))
@interface MyConvert {
}

class Api {
    @HttpRequest(url = "#{body}")
    @MyConvert
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 直接标注 `@ConvertMetaType` 的自定义注解按同名属性传递。 */
    fun testBodyTypeFromCustomMetaAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

@ConvertMetaType
@interface MyMetaType {
    Class<?> value() default Object.class;
}

class Api {
    @HttpRequest(url = "#{body}")
    @MyMetaType(Item.class)
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** `type()` 中的 `T(X)` 可静态解析为具体类型。 */
    fun testBodyTypeFromConvertMetaTypeExpression() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body}")
    @ConvertMetaType(type = "T(p.Item)")
    void fetch() {
    }
}

class Item {
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Item", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** `typeFunc()` 的运行期结果无法静态确定，`$body$` 保持内置的 Object。 */
    fun testBodyTypeStaysObjectForTypeFunc() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body}")
    @ConvertMetaType(typeFunc = "getMetaType()")
    void fetch() {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{body}"))
        assertEquals("Object", context.find("\$body\$", SpelSymbolKind.ROOT_VARIABLE).single().type)
    }

    /** 函数来自 `HttpClientProxyObjectFactory` 的注册调用与函数类的 public static 展开。 */
    fun testRuntimeDiscoversFunctions() {
        val symbols = SpelRuntimeSymbols.functions(project)

        val time = symbols.first { it.name == "time" }
        assertEquals(SpelSymbolKind.FUNCTION, time.kind)
        assertEquals("time()", time.signature)
        assertEquals("long", time.type)
        assertEquals(SpelSymbolOrigin.BUILTIN, time.origin)
        assertTrue(time.registered)
        assertEquals("获取当前时间毫秒", time.description)

        val formatDate = symbols.first { it.name == "format_date" }
        assertEquals(SpelSymbolKind.FUNCTION, formatDate.kind)
        assertTrue(formatDate.description!!.contains("别名"))
    }

    fun testBuiltinSymbolsArePartOfContext() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.PrintLog;

class Api {
    @PrintLog(reqCondition = "#{x}")
    void fetch() {
    }
}
"""
        )
        val context = SpelContextBuilder().build(literalOf("#{x}"))

        assertTrue(context.known)
        assertEquals(1, context.find("time", SpelSymbolKind.FUNCTION).size)
        assertEquals(1, context.find("\$status\$", SpelSymbolKind.ROOT_VARIABLE).size)
        // 请求阶段的条件属性可以引用请求变量，响应变量在上下文里可见但阶段不可用
        assertEquals(
            SpelVisibility.STAGE_UNAVAILABLE,
            context.find("\$status\$").single().visibility
        )
    }

}
