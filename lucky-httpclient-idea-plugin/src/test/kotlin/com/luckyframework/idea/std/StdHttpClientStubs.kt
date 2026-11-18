package com.luckyframework.idea.std

/**
 * `@StdHttpClient` 测试桩。
 *
 * 只保留 P1 依赖的最小结构：`@StdHttpClient#configId` 通过 `@AliasFor`
 * 覆盖 `@ApiConfig#value`，方法上的 `@Api#value` 决定 apiId。
 *
 * `@AliasFor` 与 `HttpRequest` 由 [com.luckyframework.idea.spel.metadata.SpelFrameworkStubs]
 * 提供，测试基类会一并加载。
 */
object StdHttpClientStubs {

    val STUBS: Map<String, String> = mapOf(
        "io/github/lucklike/httpclient/std/StdHttpClient.java" to """
package io.github.lucklike.httpclient.std;

import com.luckyframework.httpclient.proxy.configapi.ApiConfig;
import org.springframework.core.annotation.AliasFor;

@ApiConfig
public @interface StdHttpClient {

    @AliasFor(annotation = ApiConfig.class, attribute = "value")
    String configId() default "";
}
""",
        "com/luckyframework/httpclient/proxy/configapi/ApiConfig.java" to """
package com.luckyframework.httpclient.proxy.configapi;

public @interface ApiConfig {

    String value() default "";
}
""",
        "com/luckyframework/httpclient/proxy/configapi/Api.java" to """
package com.luckyframework.httpclient.proxy.configapi;

public @interface Api {

    String value();
}
"""
    )
}
