package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.context.Context;
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
     * 使用 SpEL 表达式返回一个{@link ConfigurableRecordReplay.Configuration}对象
     */
    String value();

    /**
     * SpEL 函数类
     */
    class Functions {

        /**
         * 计算 SpEL 表达式，返回一个{@link ConfigurableRecordReplay.Configuration}对象
         *
         * @param context 上下文对象
         * @return {@link ConfigurableRecordReplay.Configuration}对象
         */
        @FunctionAlias("record_replay_config")
        public static Configuration recordReplayConfig(Context context) {
            ConfigurableRecordReplay ann = context.getMergedAnnotationCheckParent(ConfigurableRecordReplay.class);
            return context.parseExpression(ann.value(), Configuration.class);
        }
    }

    /**
     * 记录不匹配时的策略
     */
    enum MismatchStrategy {
        /**
         * 随机返回一条其他的
         */
        RANDOM_ONE,

        /**
         * 调用真实环境
         */
        USE_TARGET;
    }

    /**
     * 录制模式
     */
    enum Model {
        /**
         * 录制模式
         */
        RECORD,

        /**
         * 回放模式
         */
        REPLAY,

        /**
         * 关闭录制回放
         */
        OFF;
    }

    /**
     * 配置类
     */
    class Configuration {
        /**
         * 录制模式
         */
        private Model model = Model.OFF;

        /**
         * 回放时不匹配时的策略
         */
        private MismatchStrategy replayMismatchStrategy = MismatchStrategy.USE_TARGET;

        /**
         * 录制条件
         */
        private String recordConditions = "#{$contentLength$ < 1048576}";

        /**
         * 记录ID
         */
        private String recordId = "#{__args_to_string__($mc$)}";

        /**
         * 录制文件存放位置
         */
        private String recordDir = "#{T(System).getProperty('user.dir')}/@RecordReplay";

        /**
         * 方法ID
         */
        private String methodId = "#{$method$.getName()}";

        /**
         * 录制的最大数量
         */
        private Integer recordMaxCount = 10;

        /**
         * 指定异步任务的执行器（支持SpEL表达式）
         */
        private String recordExecutor = "";

        /**
         * 回放时是否模拟延时
         */
        private boolean replayDelayMock = false;

        /**
         * 获取录制模式
         *
         * @return 录制模式
         */
        public Model getModel() {
            return model;
        }

        /**
         * 设置录制模式
         *
         * @param model 录制模式
         */
        public void setModel(Model model) {
            this.model = model;
        }

        /**
         * 获取回放时不匹配时的策略
         *
         * @return 回放时不匹配时的策略
         */
        public MismatchStrategy getReplayMismatchStrategy() {
            return replayMismatchStrategy;
        }

        /**
         * 回放时不匹配时的策略
         *
         * @param replayMismatchStrategy 回放时不匹配时的策略
         */
        public void setReplayMismatchStrategy(MismatchStrategy replayMismatchStrategy) {
            this.replayMismatchStrategy = replayMismatchStrategy;
        }

        /**
         * 获取录制条件
         *
         * @return 录制条件
         */
        public String getRecordConditions() {
            return recordConditions;
        }

        /**
         * 设置录制条件
         *
         * @param recordConditions 录制条件
         */
        public void setRecordConditions(String recordConditions) {
            this.recordConditions = recordConditions;
        }

        /**
         * 获取记录ID
         *
         * @return 记录ID
         */
        public String getRecordId() {
            return recordId;
        }

        /**
         * 设置记录ID
         *
         * @param recordId 记录ID
         */
        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        /**
         * 获取录制文件存放位置
         *
         * @return 录制文件存放位置
         */
        public String getRecordDir() {
            return recordDir;
        }

        /**
         * 设置录制文件存放位置
         *
         * @param recordDir 录制文件存放位置
         */
        public void setRecordDir(String recordDir) {
            this.recordDir = recordDir;
        }

        /**
         * 获取方法ID
         *
         * @return 方法ID
         */
        public String getMethodId() {
            return methodId;
        }

        /**
         * 设置方法ID
         *
         * @param methodId 方法ID
         */
        public void setMethodId(String methodId) {
            this.methodId = methodId;
        }

        /**
         * 获取录制的最大数量
         *
         * @return 录制的最大数量
         */
        public Integer getRecordMaxCount() {
            return recordMaxCount;
        }

        /**
         * 设置录制的最大数量
         *
         * @param recordMaxCount 录制的最大数量
         */
        public void setRecordMaxCount(Integer recordMaxCount) {
            this.recordMaxCount = recordMaxCount;
        }

        /**
         * 获取指定异步任务的执行器
         *
         * @return 指定异步任务的执行器
         */
        public String getRecordExecutor() {
            return recordExecutor;
        }

        /**
         * 设置指定异步任务的执行器
         *
         * @param recordExecutor 指定异步任务的执行器
         */
        public void setRecordExecutor(String recordExecutor) {
            this.recordExecutor = recordExecutor;
        }

        /**
         * 获取回放时是否模拟延时
         *
         * @return 回放时是否模拟延时
         */
        public boolean isReplayDelayMock() {
            return replayDelayMock;
        }

        /**
         * 设置回放时是否模拟延时
         *
         * @param replayDelayMock 回放时是否模拟延时
         */
        public void setReplayDelayMock(boolean replayDelayMock) {
            this.replayDelayMock = replayDelayMock;
        }
    }
}
