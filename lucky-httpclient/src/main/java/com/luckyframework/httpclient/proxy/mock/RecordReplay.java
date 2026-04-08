package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import com.luckyframework.httpclient.proxy.function.RandomFunctions;
import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Rar;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.hook.AsyncHook;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;
import com.luckyframework.io.FileUtils;
import com.luckyframework.spel.LazyValue;
import com.luckyframework.threadpool.ThreadPoolFactory;
import com.luckyframework.threadpool.ThreadPoolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.function.SerializationFunctions._json;
import static com.luckyframework.httpclient.proxy.function.SerializationFunctions.json;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RESPONSE_TIME_SPENT$_;

/**
 * 录制与回放
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Mock(enable = "#{__mock_enable__($mc$, __record_file_info__, __record_count__)}", mockResp = "#{__replay__($mc$, __record_file_info__)}")
@SpELImport({RecordReplay.RecordFunction.class, RecordReplay.MockFunction.class})
public @interface RecordReplay {

    //-----------------------------------------------
    //                  录制模式
    //-----------------------------------------------

    /**
     * 录制模式
     */
    String RECORD = "RECORD";

    /**
     * 回放模式
     */
    String REPLAY = "REPLAY";

    /**
     * 关闭录制回放
     */
    String OFF = "OFF";

    //-----------------------------------------------
    //            回放时请求不匹配时的策略
    //-----------------------------------------------

    /**
     * 随机返回一条其他的
     */
    String RANDOM_ONE = "RANDOM_ONE";

    /**
     * 调用真实环境
     */
    String USE_TARGET = "USE_TARGET";


    /**
     * 模式，支持 SpEL 表达式
     * <pre>
     *     RECORD -> 录制模式
     *     REPLAY -> 回放模式
     *     OFF    -> 关闭
     * </pre>
     */
    String mode();

    /**
     * 录制条件，响应体小于 1M 时才录制
     */
    String recordConditions() default "#{$contentLength$ < 1048576}";

    /**
     * 定义一个可以唯一标识每一次请求的唯一标识，支持 SpEL 表达式
     */
    String recordId() default "#{__args_to_string__($mc$)}";

    /**
     * 录制文件的存放目录
     */
    String recordDir() default "#{T(System).getProperty('user.dir')}/@RecordReplay";

    /**
     * 方法ID
     */
    String methodId() default "#{$method$.getName()}";

    /**
     * 录制的最大数量
     */
    String recordMaxCount() default "10";

    /**
     * <pre>
     *   指定异步任务的执行器（支持SpEL表达式）
     *     1.如果表达式结果类型为{@link Executor}时直接使用该执行器
     *     2.如果表达式结果类型为{@link ThreadPoolParam}时，使用{@link ThreadPoolFactory#createThreadPool(ThreadPoolParam)}来创建执行器
     *     3.如果表达式结果类型为{@link String}时，使用{@link HttpClientProxyObjectFactory#getAlternativeAsyncExecutor(String)}来获取执行器
     *     4.返回结果为其他类型时将报错
     * </pre>
     */
    String recordExecutor() default "";

    /**
     * 回放时请求不匹配时的策略, 支持SpEL表达式
     */
    String replayMismatchStrategy() default USE_TARGET;

    /**
     * 回放时是否模拟延时, 支持SpEL表达式，返回值为boolean类型
     */
    String replayDelayMock() default "false";

    /**
     * 记录实体类
     */
    class Record {
        private String id;
        private Integer status;
        private Date startTime;
        private Date endTime;
        private Long executionTime;
        private Map<String, List<Object>> headers;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Map<String, List<Object>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<Object>> headers) {
            this.headers = headers;
        }

        public Date getStartTime() {
            return startTime;
        }

        public void setStartTime(Date startTime) {
            this.startTime = startTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public void setEndTime(Date endTime) {
            this.endTime = endTime;
        }

        public Long getExecutionTime() {
            return executionTime;
        }

        public void setExecutionTime(Long executionTime) {
            this.executionTime = executionTime;
        }
    }

    /**
     * 记录文件信息
     */
    class RecordFileInfo {

        /**
         * 录制文件名
         */
        public static final String RECORD_HEADER = "record.h";

        /**
         * 响应体文件名
         */
        public static final String RECORD_BODY = "record.b";

        private final File recordDir;

        public RecordFileInfo(Context context) {
            this.recordDir = getRecordDir(context);
        }

        public File getRecordDir() {
            return recordDir;
        }

        /**
         * 获取录制文件
         *
         * @param recordId 记录 ID
         * @return 录制文件
         */
        public TempPair<File, File> getRecordFile(String recordId) {
            File methodDir = new File(recordDir, recordId);
            return TempPair.of(new File(methodDir, RECORD_HEADER), new File(methodDir, RECORD_BODY));
        }

        /**
         * 获取录制文件，如果对应 ID 的录制文件不存在，则随机返回一个录制文件
         *
         * @param recordId 记录 ID
         * @return 录制文件
         */
        public TempPair<File, File> getRecordFileNonExistsRandomReturn(String recordId) {
            if (recordFileIsExists(recordId)) {
                return getRecordFile(recordId);
            }

            File[] files = recordDir.listFiles(f -> {
                if (f.isFile()) {
                    return false;
                }
                File[] sfArray = f.listFiles(sf -> sf.isFile() && (RECORD_HEADER.equals(sf.getName()) || RECORD_BODY.equals(sf.getName())));
                if (ContainerUtils.isEmptyArray(sfArray)) {
                    return false;
                }
                return sfArray.length == 2;
            });

            assert files != null;
            File file = files[RandomFunctions.randomInt(files.length - 1)];

            return TempPair.of(new File(file, RECORD_HEADER), new File(file, RECORD_BODY));
        }

        /**
         * 当前方法的记录文件夹是否存在
         *
         * @return 当前方法的记录文件夹是否存在
         */
        public boolean recordDirIsExists() {
            return recordDir.exists();
        }

        /**
         * 记录文件是否存在
         *
         * @param recordId 记录 ID
         * @return 记录文件是否存在
         */
        public boolean recordFileIsExists(String recordId) {
            TempPair<File, File> pair = getRecordFile(recordId);
            boolean recordFileExists = pair.getOne().isFile() && pair.getOne().exists();
            boolean bodyFileExists = pair.getTwo().isFile() && pair.getTwo().exists();
            return recordFileExists && bodyFileExists;
        }

        /**
         * 获取当前方法已存在的记录文件的数量
         *
         * @return 当前方法已存在的记录文件的数量
         */
        public int getRecordFileCount() {
            //  文件夹不存在
            if (!recordDirIsExists()) {
                return 0;
            }

            // 不存在子目录
            File[] files = recordDir.listFiles();
            if (ContainerUtils.isEmptyArray(files)) {
                return 0;
            }

            return Math.toIntExact(Stream.of(files)
                    .filter(f -> {
                        if (f.isFile()) {
                            return false;
                        }
                        File[] sfArray = f.listFiles(sf -> sf.isFile() && (RECORD_HEADER.equals(sf.getName()) || RECORD_BODY.equals(sf.getName())));
                        if (ContainerUtils.isEmptyArray(sfArray)) {
                            return false;
                        }
                        return sfArray.length == 2;
                    }).count());
        }


        /**
         * 获取当前请求对应的录制资源
         *
         * @param context 上下文
         * @return 录制资源
         */
        private File getRecordDir(Context context) {
            RecordReplay ann = CommonFunction.getAnn(context);
            String dir = ann.recordDir();
            String dirPath = context.parseExpression(dir, String.class);
            String classPath = context.lookupContext(ClassContext.class).getCurrentAnnotatedElement().getName();
            String methodId = context.parseExpression(ann.methodId(), String.class);
            return new File(dirPath, StringUtils.joinUrlPath(classPath, methodId));
        }

    }

    /**
     * 公共函数
     */
    class CommonFunction {


        /**
         * 存储记录文件数量的变量名
         */
        public static final String RECORD_COUNT = "__record_count__";

        /**
         * 存储记录信息对象
         */
        public static final String RECORD_FILE_INFO = "__record_file_info__";

        /**
         * 用于写记录文件的线程池
         */
        public static final String RECORD_EXECUTOR = "__record_executor__";

        /**
         * 获取自动录制与回放的注解
         *
         * @param context 上下文
         * @return 注解实例
         */
        public static RecordReplay getAnn(Context context) {
            return context.getMergedAnnotationCheckParent(RecordReplay.class);
        }

        /**
         * 获取录制 ID
         *
         * @param mc  方法上下文
         * @param ann 注解实例
         * @return 录制 ID
         */
        public static String getRecordId(MethodContext mc, RecordReplay ann) {
            return mc.parseExpression(ann.recordId(), String.class);
        }
    }

    /**
     * Mock函数
     */
    class MockFunction {

        /**
         * 是否使用 Mock 功能
         *
         * @param mc             方法上下文
         * @param recordFileInfo 记录文件信息
         * @param recordCount    记录文件数量
         * @return 是否使用 Mock 功能
         */
        @FunctionAlias("__mock_enable__")
        public static boolean enableMock(MethodContext mc, RecordFileInfo recordFileInfo, AtomicInteger recordCount) throws Exception {
            RecordReplay ann = CommonFunction.getAnn(mc);

            // 非回放模式
            if (!Objects.equals(mc.parseExpression(ann.mode(), String.class), REPLAY)) {
                return false;
            }

            // 记录文件数量小于 1
            if (recordCount.get() < 1) {
                return false;
            }

            // 获取录制 ID 和录制文件
            String recordId = CommonFunction.getRecordId(mc, ann);
            String fileName = DigestFunctions.md5Hex(recordId);

            // 与当前方法上下文匹配的录制文件是否存在
            boolean fileIsExists = recordFileInfo.recordFileIsExists(fileName);
            if (fileIsExists) {
                return true;
            }

            // 不存在时校验策略
            String mismatchStrategy = mc.parseExpression(ann.replayMismatchStrategy(), String.class);
            return Objects.equals(mismatchStrategy, RANDOM_ONE);
        }


        /**
         * 重播录制好的响应
         *
         * @param mc 方法上下文
         * @return 模拟请求
         */
        @FunctionAlias("__replay__")
        public static MockResponse replay(MethodContext mc, RecordFileInfo recordFileInfo) throws Exception {
            RecordReplay ann = CommonFunction.getAnn(mc);

            String recordId = CommonFunction.getRecordId(mc, ann);
            String fileName = DigestFunctions.md5Hex(recordId);

            TempPair<File, File> recordFilePair = recordFileInfo.getRecordFileNonExistsRandomReturn(fileName);

            File recordFile = recordFilePair.getOne();
            File bodyFile = recordFilePair.getTwo();

            // 读记录
            Record record = _json(FileCopyUtils.copyToString(new InputStreamReader(Files.newInputStream(recordFile.toPath()))), Record.class);
            if (mc.parseExpression(ann.replayDelayMock(), boolean.class)) {
                Thread.sleep(record.getExecutionTime());
            }

            MockResponse mockResponse = MockResponse.create();
            mockResponse.status(record.getStatus());

            mockResponse.header("Mock-Annotation", "@AutoRecordReplay");
            mockResponse.header("Mock-Record-Id-Expression", ann.recordId());
            mockResponse.header("Mock-Record-Id-Value", recordId);
            mockResponse.header("Mock-Record-File-Dir", String.format("%s/%s,%s", fileName, RecordFileInfo.RECORD_HEADER, RecordFileInfo.RECORD_BODY));


            record.getHeaders().forEach((name, values) -> {
                for (Object value : values) {
                    mockResponse.header(name, value);
                }
            });

            mockResponse.body(Files.newInputStream(bodyFile.toPath()));

            return mockResponse;
        }
    }

    /**
     * 录制函数
     */
    class RecordFunction {

        private static final Logger log = LoggerFactory.getLogger(RecordFunction.class);

        @FunctionAlias("__record_enable__")
        public static boolean enableRecord(Context context) {
            RecordReplay ann = CommonFunction.getAnn(context);
            return Objects.equals(context.parseExpression(ann.mode(), String.class), RECORD);
        }

        @FunctionAlias("__args_to_string__")
        public static String defRecordId(MethodContext mc) {
            return Arrays.toString(mc.getArguments());
        }


        /**
         * 初始化用于记录响应文件的线程池的懒加载对象
         *
         * @param mec 方法元上下文
         * @return 记录响应文件的线程池的懒加载对象
         */
        @Callback(lifecycle = Lifecycle.METHOD_META, storeOrNot = true, storeName = CommonFunction.RECORD_EXECUTOR)
        public static LazyValue<Executor> initRecordExecutor(MethodMetaContext mec) {
            return LazyValue.of(() -> {
                String recordExecutor = CommonFunction.getAnn(mec).recordExecutor();
                if (StringUtils.hasText(recordExecutor)) {
                    return mec.createExecutor(recordExecutor);
                }
                return mec.getHttpProxyFactory().getAsyncExecutor();
            });
        }

        /**
         * 加载当前方法记录文件的数量
         *
         * @return 当前方法记录文件的数量
         */
        @Callback(lifecycle = Lifecycle.METHOD, storeOrNot = true, unfold = true)
        public static Map<String, Object> initRecordInfo(MethodContext context) {
            RecordFileInfo recordFileInfo = new RecordFileInfo(context);
            Map<String, Object> map = new HashMap<>();
            map.put(CommonFunction.RECORD_FILE_INFO, recordFileInfo);
            map.put(CommonFunction.RECORD_COUNT, new AtomicInteger(recordFileInfo.getRecordFileCount()));
            return map;
        }


        @AsyncHook("#{__record_executor__}")
        @Callback(enable = "#{__record_enable__($CC$)}", lifecycle = Lifecycle.DESTROY, errorInterrupt = false)
        public static void record(MethodContext mc,
                                  Response response,
                                  @Rar(CommonFunction.RECORD_FILE_INFO) RecordFileInfo recordFileInfo,
                                  @Rar(_$RESPONSE_TIME_SPENT$_) ResponseTimeSpent timeSpent,
                                  @Rar(CommonFunction.RECORD_COUNT) AtomicInteger recordCount) throws Exception {
            RecordReplay ann = CommonFunction.getAnn(mc);

            // 超过了最大录制数量
            int max = mc.parseExpression(ann.recordMaxCount(), int.class);
            if (recordCount.get() > (max - 1)) {
                return;
            }

            // 不满足录制条件
            if (!mc.parseExpression(ann.recordConditions(), boolean.class)) {
                return;
            }

            // 获取录制 ID 和录制文件
            String recordId = CommonFunction.getRecordId(mc, ann);
            String fileName = DigestFunctions.md5Hex(recordId);

            TempPair<File, File> recordFilePair = recordFileInfo.getRecordFile(fileName);

            File recordFile = recordFilePair.getOne();
            File bodyFile = recordFilePair.getTwo();

            if (recordFile.exists()) {
                return;
            }

            log.info("<Recording_>[{}][{}][{}] Start recording the response file {} ",
                    recordCount.get() + 1,
                    mc.getApiDescribe().getName(),
                    response.getRequest().getUniqueId(),
                    recordFile.getParentFile().getAbsoluteFile());

            // 录制
            doRecord(response, timeSpent, recordId, recordFile, bodyFile);

            log.info("<Completion>[{}][{}][{}] Response file {} records completion",
                    recordCount.get() + 1,
                    mc.getApiDescribe().getName(),
                    response.getRequest().getUniqueId(),
                    recordFile.getParentFile().getAbsoluteFile()
            );

            // 记录数量
            recordCount.incrementAndGet();
        }

        /**
         * 生成录制文件
         *
         * @param response   响应对象
         * @param timeSpent  执行时间相关
         * @param recordId   记录 ID
         * @param recordFile 记录文件
         * @param bodyFile   响应体文件
         */
        private static void doRecord(Response response, ResponseTimeSpent timeSpent, String recordId, File recordFile, File bodyFile) throws Exception {
            Record record = new Record();
            record.setId(recordId);
            record.setStatus(response.getStatus());
            record.setStartTime(timeSpent.getStartDate());
            record.setEndTime(timeSpent.getEndDate());
            record.setExecutionTime(timeSpent.getExeTime());
            Map<String, List<Object>> headers = new HashMap<>();
            for (Header header : response.getHeaderManager().getHeaders()) {
                String name = header.getName();
                List<Object> headerValues = headers.computeIfAbsent(name, k -> new ArrayList<>());
                headerValues.add(header.getValue());
            }
            record.setHeaders(headers);

            // 创建父级文件夹
            FileUtils.createSaveFolder(recordFile.getParentFile());

            // 创建录制文件
            FileCopyUtils.copy(json(record).getBytes(StandardCharsets.UTF_8), recordFile);
            FileCopyUtils.copy(response.getInputStream(), Files.newOutputStream(bodyFile.toPath()));
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

        public Model getModel() {
            return model;
        }

        public void setModel(Model model) {
            this.model = model;
        }

        public MismatchStrategy getReplayMismatchStrategy() {
            return replayMismatchStrategy;
        }

        public void setReplayMismatchStrategy(MismatchStrategy replayMismatchStrategy) {
            this.replayMismatchStrategy = replayMismatchStrategy;
        }

        public String getRecordConditions() {
            return recordConditions;
        }

        public void setRecordConditions(String recordConditions) {
            this.recordConditions = recordConditions;
        }

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        public String getRecordDir() {
            return recordDir;
        }

        public void setRecordDir(String recordDir) {
            this.recordDir = recordDir;
        }

        public String getMethodId() {
            return methodId;
        }

        public void setMethodId(String methodId) {
            this.methodId = methodId;
        }

        public Integer getRecordMaxCount() {
            return recordMaxCount;
        }

        public void setRecordMaxCount(Integer recordMaxCount) {
            this.recordMaxCount = recordMaxCount;
        }

        public String getRecordExecutor() {
            return recordExecutor;
        }

        public void setRecordExecutor(String recordExecutor) {
            this.recordExecutor = recordExecutor;
        }

        public boolean isReplayDelayMock() {
            return replayDelayMock;
        }

        public void setReplayDelayMock(boolean replayDelayMock) {
            this.replayDelayMock = replayDelayMock;
        }
    }
}
