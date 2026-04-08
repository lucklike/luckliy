package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 可配置的录制回放注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/4/8 22:44
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RecordReplay(
        mode = "#{record_replay_config($CC$).model.toString()}",
        replayMismatchStrategy = "#{record_replay_config($CC$).replayMismatchStrategy.toString()}",
        recordConditions = "``@max(2):#{record_replay_config($CC$).recordConditions}``",
        recordId = "``@max(2):#{record_replay_config($CC$).recordId}``",
        recordDir = "``@max(2):#{record_replay_config($CC$).recordDir}``",
        methodId = "``@max(2):#{record_replay_config($CC$).methodId}``",
        recordExecutor = "``@max(2):#{record_replay_config($CC$).recordExecutor}``",
        recordMaxCount = "#{record_replay_config($CC$).recordMaxCount}",
        replayDelayMock = "#{record_replay_config($CC$).replayDelayMock}"
)
@SpELImport(ConfigurableRecordReplay.Functions.class)
public @interface ConfigurableRecordReplay {

    /**
     * 使用 SpEL 表达式返回一个{@link RecordReplay.Configuration}对象
     */
    String value();

    /**
     * SpEL 函数类
     */
    class Functions {

        /**
         * 计算 SpEL 表达式，返回一个{@link RecordReplay.Configuration}对象
         *
         * @param context 上下文对象
         * @return {@link RecordReplay.Configuration}对象
         */
        @FunctionAlias("record_replay_config")
        public static RecordReplay.Configuration recordReplayConfig(Context context) {
            ConfigurableRecordReplay ann = context.getMergedAnnotationCheckParent(ConfigurableRecordReplay.class);
            return context.parseExpression(ann.value(), RecordReplay.Configuration.class);
        }

    }
}
