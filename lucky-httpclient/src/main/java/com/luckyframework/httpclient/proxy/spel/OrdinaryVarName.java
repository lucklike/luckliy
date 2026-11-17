package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;

/**
 * 普通（非Root）变量名常量。
 *
 * <p>这里的变量运行期注册在普通变量空间，必须以{@code #name}形式引用。
 */
@LuckyComponent(type = LuckyComponentType.CONFIG)
public class OrdinaryVarName {

    /** 响应耗时统计。 */
    @SpelSymbol(type = ResponseTimeSpent.class, from = Lifecycle.RESPONSE_INIT)
    public static final String _$RESPONSE_TIME_SPENT$_ = "_$responseTimeSpent$_";

    /** 当前重试序号。 */
    @SpelSymbol(type = Integer.class, from = Lifecycle.RETRY)
    public static final String _$RETRY_COUNT$_ = "_$retryCount$_";

    /** 重试任务的结果。 */
    @SpelSymbol(typeExpression = "com.luckyframework.retry.TaskResult", from = Lifecycle.RETRY)
    public static final String _$TASK_RESULT$_ = "_$taskResult$_";

    /** 响应对象是否已被修改。 */
    @SpelSymbol(type = Boolean.class, from = Lifecycle.RESPONSE)
    public static final String _$RESPONSE_CHANGE$_ = "_$responseChange$_";
}
