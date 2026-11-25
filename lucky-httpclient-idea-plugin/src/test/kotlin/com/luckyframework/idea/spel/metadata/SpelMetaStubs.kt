package com.luckyframework.idea.spel.metadata

/**
 * `com.luckyframework.httpclient.proxy.spel.meta` 元注解的源码桩。
 *
 * 插件在运行期读取这些注解来推导 SpEL 语义，因此凡是需要覆盖
 * "元注解驱动"路径的测试用例都要先注册它们。
 */
object SpelMetaStubs {

    val STUBS: Map<String, String> = mapOf(
        "com/luckyframework/httpclient/proxy/spel/hook/Lifecycle.java" to """
package com.luckyframework.httpclient.proxy.spel.hook;

public enum Lifecycle {
    CLASS,
    METHOD_META,
    METHOD,
    REQUEST_INIT,
    CONFIG_API_INIT,
    CONFIG_API_METHOD_INIT,
    CONFIG_API_INIT_COMPLETE,
    REQUEST,
    INVOKE_WRAPPER_METHOD,
    RETRY,
    RESPONSE_INIT,
    RESPONSE,
    METHOD_RESULT,
    THROWABLE,
    DESTROY,
    NON
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelKind.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public enum SpelKind {
    TEMPLATE,
    KEY_VALUE,
    DECLARATION,
    CONDITION,
    FUNCTION,
    LITERAL,
    NONE,
    EXPRESSION,
    EXPRESSION_LIST,
    MAP_EXPRESSION,
    DATA,
    STRUCT,
    PARAMETER_INJECTION_VAR,
    PARAMETER_INJECTION_EXPRESSION
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelConstraint.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public enum SpelConstraint {
    ANY,
    NOT_NULL
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelAttribute.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;

public @interface SpelAttribute {

    SpelKind kind() default SpelKind.TEMPLATE;

    Lifecycle stage() default Lifecycle.METHOD;

    Class<?>[] result() default {};

    String[] typeExpression() default {};

    SpelConstraint[] constraint() default {};

    String separator() default "";

    boolean keySpel() default false;

    boolean valueSpel() default true;

    String condition() default "";

    String key() default "";

    boolean expandable() default false;

    Class<?> resultType() default Object.class;

    String description() default "";
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelSeparator.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public @interface SpelSeparator {
}
""",
        "com/luckyframework/httpclient/proxy/spel/VarType.java" to """
package com.luckyframework.httpclient.proxy.spel;

public enum VarType {
    ROOT,
    ENVIRONMENT,
    BEAN,
    NORMAL
}
""",
        "com/luckyframework/httpclient/proxy/spel/Var.java" to """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface Var {

    @SpelAttribute(kind = SpelKind.PARAMETER_INJECTION_VAR)
    String value() default "";

    VarType type() default VarType.NORMAL;
}
""",
        "com/luckyframework/httpclient/proxy/spel/Rar.java" to """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

@Var(type = VarType.ROOT)
public @interface Rar {

    @AliasFor(annotation = Var.class, attribute = "value")
    @SpelAttribute(kind = SpelKind.PARAMETER_INJECTION_VAR)
    String value() default "";
}
""",
        "com/luckyframework/httpclient/proxy/spel/Env.java" to """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

@Var(type = VarType.ENVIRONMENT)
public @interface Env {

    @AliasFor(annotation = Var.class, attribute = "value")
    @SpelAttribute(kind = SpelKind.PARAMETER_INJECTION_VAR)
    String value() default "";
}
""",
        "com/luckyframework/httpclient/proxy/spel/Bean.java" to """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

@Var(type = VarType.BEAN)
public @interface Bean {

    @AliasFor(annotation = Var.class, attribute = "value")
    @SpelAttribute(kind = SpelKind.PARAMETER_INJECTION_VAR)
    String value() default "";
}
""",
        "com/luckyframework/reflect/Param.java" to """
package com.luckyframework.reflect;

public @interface Param {

    String value() default "";

    String def() default "null";
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelStage.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public @interface SpelStage {
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelName.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public @interface SpelName {
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelSymbolKind.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public enum SpelSymbolKind {
    AUTO,
    ROOT,
    VAR,
    BEAN,
    ENV,
    INTERNAL,
    FUNCTION,
    PARAM
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelSymbol.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;

public @interface SpelSymbol {

    String name() default "";

    SpelSymbolKind kind() default SpelSymbolKind.AUTO;

    Class<?> type() default Object.class;

    String typeExpression() default "";

    Lifecycle from() default Lifecycle.METHOD;

    Lifecycle until() default Lifecycle.NON;
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/LuckyComponentType.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public enum LuckyComponentType {
    AUTO,
    API,
    DATABASE,
    CONFIG,
    FUNCTION,
    SUPPORT
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/LuckyComponent.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public @interface LuckyComponent {

    LuckyComponentType type() default LuckyComponentType.AUTO;

    String group() default "httpclient";

    double priority() default Double.MAX_VALUE;
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelNaming.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public enum SpelNaming {
    AS_IS,
    CAMEL_CASE,
    KEBAB_CASE,
    SNAKE_CASE
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/SpelConfig.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

@LuckyComponent(type = LuckyComponentType.CONFIG, group = "config", priority = 4)
public @interface SpelConfig {

    String prefix();

    String[] aliases() default {};

    SpelNaming naming() default SpelNaming.AS_IS;

    ConfigScope scope() default ConfigScope.BOTH;

    String description() default "";
}
""",
        "com/luckyframework/httpclient/proxy/spel/meta/ConfigScope.java" to """
package com.luckyframework.httpclient.proxy.spel.meta;

public enum ConfigScope {
    CLASS,
    METHOD,
    BOTH
}
"""
    )
}
