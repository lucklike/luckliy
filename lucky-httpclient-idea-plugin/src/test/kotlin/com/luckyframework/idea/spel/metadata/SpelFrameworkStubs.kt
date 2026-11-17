package com.luckyframework.idea.spel.metadata

/**
 * lucky-httpclient 框架注解的源码桩。
 *
 * 插件的 SpEL 属性语义完全从注解声明上的 `@SpelAttribute` 实时推导，
 * 桩必须与真实源码保持一致：真实源码中标注了 `@SpelAttribute` 的属性
 * 在桩里漏标，会被判为「运行期不求值」，高亮、补全与检查全部跳过；
 * 反之多标会让不该解析的属性出现诊断。
 *
 * 所有测试夹具统一从这里取框架注解桩，避免不同夹具对同一个注解
 * 给出不同语义（例如 `HttpRequest` 一套带 `@SpelAttribute`、一套不带）。
 *
 * 只保留与 SpEL 语义相关的元素，省略与求值无关的属性。
 */
object SpelFrameworkStubs {

    const val HTTP_REQUEST_PATH =
        "com/luckyframework/httpclient/proxy/annotations/HttpRequest.java"

    const val SPEL_IMPORT_PATH = "com/luckyframework/httpclient/proxy/spel/SpELImport.java"

    /** 与真实 `HttpRequest` 一致：`value` 别名到 `url`，二者都参与 SpEL。 */
    const val HTTP_REQUEST = """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

public @interface HttpRequest {

    @AliasFor("url")
    String value() default "";

    @AliasFor("value")
    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String func() default "";
}
"""

    /**
     * 与真实 `SpELImport` 一致：`root`/`var` 的键参与 SpEL（`keySpel`），
     * `rootLit`/`varLit` 是字面量，`separator` 由 `@SpelSeparator` 标记。
     */
    const val SPEL_IMPORT = """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSeparator;

public @interface SpELImport {

    Class<?>[] value() default {};

    If[] ifs() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, keySpel = true)
    String[] root() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, keySpel = true)
    String[] var() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, valueSpel = false)
    String[] rootLit() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, valueSpel = false)
    String[] varLit() default {};

    String[] pack() default {};

    @SpelSeparator
    String separator() default "=";
}
"""

    val STUBS: Map<String, String> = mapOf(
        HTTP_REQUEST_PATH to HTTP_REQUEST,
        SPEL_IMPORT_PATH to SPEL_IMPORT,
        "com/luckyframework/httpclient/proxy/spel/SpELImports.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface SpELImports {
    SpELImport[] value();
}
""",
        "com/luckyframework/httpclient/proxy/spel/If.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface If {
    String condition();
    Class<?>[] classes();
}
""",
        "com/luckyframework/httpclient/proxy/spel/FunctionAlias.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface FunctionAlias {
    String value();
}
""",
        "org/springframework/core/annotation/AliasFor.java" to """
package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

public @interface AliasFor {
    String value() default "";
    Class<? extends Annotation> annotation() default Annotation.class;
    String attribute() default "";
}
"""
    )
}
