package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AliasFor;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分片文件上传
 * 负责把本地大文件按分片上传到服务端的注解<br/>
 * <pre>
 *     使用该注解的方法的返回值必须是以下类型：
 *     1.{@link ChunkUploadResult}（完整的上传结果，含成功/跳过/失败的分片明细）
 *     2.{@link Boolean}或{@code boolean}（整个流程是否成功，等价于{@link ChunkUploadResult#isSuccess()}）
 *     3.<b>void</b>（不关心结果时使用；本方法仍然阻塞，需要异步时由调用方自己提交到线程池）
 *
 *     上传的文件来源按以下顺序确定：
 *     1.{@link #file()}配置的表达式，结果必须是{@link File}或文件路径字符串，例如{@code #{file}}、{@code D:/data/big.zip}；
 *     2.{@link #file()}为空时，取方法参数列表中第一个{@link File}类型的参数值。
 *
 *     服务端协议由{@link ChunkUploadApi}的实现接口声明，{@link #api()}指定该接口：
 *     1.不指定时（保持默认值{@link ChunkUploadApi}），使用声明当前方法的接口自身，
 *       此时该接口必须继承{@link ChunkUploadApi}，即上传协议方法与{@code @ChunkUpload}方法写在同一个接口里；
 *     2.指定其他接口时，协议方法与被注解方法可以分开声明。
 *
 *     完整用法：
 *     {@code
 *     @ServerAddress("http://localhost:8864/shard/upload/")
 *     public interface FileUploadApi extends ChunkUploadApi {
 *
 *         // 判断分片是否已上传：先查服务端已有的片号集合，再与当前分片对账（断点续传）
 *         @Override
 *         default boolean isChunkUploaded(String fileId, FileChunk fileChunk) {
 *             return uploadedChunks(fileId).contains(fileChunk.getIndex());
 *         }
 *
 *         @Get("uploadChunks")
 *         @RespConvert("#{$body$.data}")
 *         List<Long> uploadedChunks(@Query("fileId") String fileId);
 *
 *         // 上传单个分片：协议接口要求入参是FileChunk，签名不一致时用default方法拆解
 *         @Override
 *         default boolean uploadChunk(String fileId, FileChunk fileChunk) {
 *             return uploadChunkForm(fileId, fileChunk.getIndex(), fileChunk.getContent());
 *         }
 *
 *         @Post("chunk")
 *         boolean uploadChunkForm(@MultiData("fileId") String fileId,
 *                                 @MultiData("chunkNumber") long chunkNumber,
 *                                 @MultiFile(value = "file", fileName = "chunk") byte[] content);
 *
 *         // 通知服务端合并：收到的是原始文件，文件名等信息按需从File取用再上报
 *         @Override
 *         default boolean mergeChunks(String fileId, File file, int totalChunks) {
 *             return mergeRequest(fileId, file.getName(), totalChunks);
 *         }
 *
 *         @Post("merge")
 *         boolean mergeRequest(@Query("fileId") String fileId,
 *                              @Query("fileName") String fileName,
 *                              @Query("totalChunks") int totalChunks);
 *
 *         // 声明式分片上传：协议方法在本接口上，无需再指定api
 *         @ChunkUpload(chunkSize = "1048576", maxRetryCount = "5")
 *         ChunkUploadResult upload(File file);
 *     }
 *     }
 *
 *     需要把上传逻辑与普通接口分开声明时，在另一个接口上实现{@link ChunkUploadApi}，
 *     并用{@link #api()}指定它；协议方法签名与{@link ChunkUploadApi}不一致时，
 *     可以在实现接口上用default方法做参数拆解（如从{@code FileChunk}取索引与内容）。
 *
 *     调用后必须判断结果，因为分片失败、重试耗尽、合并失败都不会抛异常：
 *     {@code
 *     FileUploadApi api = new HttpClientProxyObjectFactory().getProxyObject(FileUploadApi.class);
 *     ChunkUploadResult result = api.upload(new File("D:/big.zip"));
 *     if (!result.isSuccess()) {
 *         log.warn("Chunks not uploaded completely: {}", result.getFailedChunks());
 *     }
 *     }
 *
 *     表达式属性（{@link #file()}/{@link #fileId()}/{@link #fileName()}，以及数值型的
 *     {@link #chunkSize()}/{@link #maxConcurrency()}/{@link #maxRetryCount()}/{@link #retryBackoffMillis()}）
 *     都支持SpEL，可引用方法参数、{@code #{$method_args$}}等上下文变量，详见{@link SpELVariableNote}。
 *     数值属性声明为{@code String}：既可以写字面量{@code "1048576"}，也可以写表达式{@code #{chunkSize}}，
 *     留空时使用{@link ChunkUploadOptions}里对应的默认值。
 *
 *     异步执行器：分片任务统一提交给{@code MethodContext#getAsyncTaskExecutor()}返回的执行器
 *     （与{@code @RangeDownload}同一个路线），因此分片上传不会自建线程池；
 *     并发数由{@link #maxConcurrency()}的提交窗口限定，需要换执行器时可在当前方法上标注
 *     {@code @AsyncExecutor}，或直接给当前方法声明一个{@code AsyncTaskExecutor}参数（优先于默认执行器）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-21 11:30
 * @see ChunkUploadApi
 * @see ChunkUploadExecutor
 * @see ChunkUploadOptions
 * @see ChunkUploadResult
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpELImport(ChunkUpload.ChunkUploadFunction.class)
@Wrapper(fun = "__chunk_upload__")
public @interface ChunkUpload {

    /**
     * 待上传文件的位置，支持SpEL表达式
     *
     * @see #file()
     * @see SpELVariableNote
     */
    @AliasFor("file")
    String value() default "";

    /**
     * 待上传文件的位置，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是{@link File}或文件路径字符串；
     *     留空时自动取方法参数列表中的第一个{@link File}参数。
     * </pre>
     *
     * @see SpELVariableNote
     */
    @AliasFor("value")
    String file() default "";

    /**
     * 文件唯一标识，支持SpEL表达式
     * <pre>
     *     用于查询已上传分片与通知合并，服务端据此判断分片归属。
     *     留空时使用文件内容的MD5值，即同一份文件重复上传时可以从断点处继续；
     *     需要业务侧自定义标识（如订单号+文件名）时在此配置。
     * </pre>
     *
     * @see SpELVariableNote
     */
    String fileId() default "";

    /**
     * 服务端保存时使用的文件名，支持SpEL表达式
     * <pre>
     *     留空时使用待上传文件的原始文件名。
     *     注意：合并协议方法{@link ChunkUploadApi#mergeChunks}收到的是原始File，
     *     上报给服务端的文件名由协议实现自己从File取用，
     *     本属性的取值只记录在{@link ChunkUploadResult#getFileName()}供调用方使用。
     * </pre>
     *
     * @see SpELVariableNote
     */
    String fileName() default "";

    /**
     * 服务端协议接口的Class
     * <pre>
     *     保持默认值{@link ChunkUploadApi}时使用声明当前方法的接口，要求该接口继承{@link ChunkUploadApi}；
     *     协议方法与上传方法分开声明时，在此指定协议接口。
     * </pre>
     *
     * @return 服务端协议接口的Class
     */
    Class<? extends ChunkUploadApi> api() default ChunkUploadApi.class;

    /**
     * 单个分片的字节数，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是数字，例如{@code "1048576"}、{@code #{chunkSize}}；
     *     留空时使用{@link ChunkUploadOptions#DEFAULT_CHUNK_SIZE}。
     * </pre>
     *
     * @return 解析后的单个分片字节数
     * @see SpELVariableNote
     */
    String chunkSize() default "";

    /**
     * 同时上传的分片数量上限，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是数字，例如{@code "4"}、{@code #{concurrency}}；
     *     留空时使用{@link ChunkUploadOptions#DEFAULT_MAX_CONCURRENCY}。
     * </pre>
     *
     * @return 解析后的最大并发数
     * @see SpELVariableNote
     */
    String maxConcurrency() default "";

    /**
     * 分片上传失败后的最大重传轮数，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是数字，例如{@code "5"}、{@code #{retryCount}}，小于0表示无限重传；
     *     留空时使用{@link ChunkUploadOptions#DEFAULT_MAX_RETRY_COUNT}。
     * </pre>
     *
     * @return 解析后的最大重传轮数
     * @see SpELVariableNote
     */
    String maxRetryCount() default "";

    /**
     * 每轮重传前的等待时间（毫秒），支持SpEL表达式
     * <pre>
     *     表达式的结果必须是数字，例如{@code "1000"}、{@code #{backoff}}；
     *     留空时使用{@link ChunkUploadOptions#DEFAULT_RETRY_BACKOFF_MILLIS}。
     * </pre>
     *
     * @return 解析后的等待时间（毫秒）
     * @see SpELVariableNote
     */
    String retryBackoffMillis() default "";

    /**
     * 是否在上传前查询并跳过服务端已有的分片
     * <pre>
     *     服务端没有提供查询能力时（{@link ChunkUploadApi#isChunkUploaded}保持默认实现）应设为false，
     *     否则每次都会全量上传，只是不会报错。
     * </pre>
     *
     * @return 跳过已有分片返回true
     */
    boolean skipUploaded() default true;

    /**
     * 是否在所有分片上传成功后自动通知服务端合并
     *
     * @return 自动合并返回true；由调用方自行决定何时合并返回false
     */
    boolean mergeAfterSuccess() default true;

    /**
     * 合并失败时是否删除服务端已上传的分片文件
     * <pre>
     *     开启后，当合并请求<b>实际发起</b>且失败（返回false或抛异常）时，
     *     框架会调用{@link ChunkUploadApi#deleteChunks}回收留在服务端的孤儿分片；
     *     分片没传完而未发起合并时不会删除，那些分片下次续传还要用；
     *     删除失败不会抛异常，只记录在{@link ChunkUploadResult}里；
     *     {@code deleteChunks}没有可兜底的默认协议（未覆写时抛{@link ChunkUploadNotImplementedException}，
     *     同样只记入结果与日志），因此开启本属性前必须覆写{@link ChunkUploadApi#deleteChunks}，
     *     服务端没有删除接口时请勿开启。
     * </pre>
     *
     * @return 合并失败后删除分片返回true；保留分片返回false
     */
    boolean deleteOnMergeFailure() default false;

    /**
     * 分片上传函数
     */
    class ChunkUploadFunction {

        private static final Logger log = LoggerFactory.getLogger(ChunkUploadFunction.class);

        /**
         * 分片上传入口，由{@code @Wrapper(fun = "__chunk_upload__")}指定
         * <pre>
         *     本方法不等价于任何HTTP请求：它先解析注解配置得到待上传文件与协议接口，
         *     再把编排工作全部交给{@link ChunkUploadExecutor}，最后把结果转成方法声明的返回值类型。
         *     由于上传流程不依赖当前方法自身的请求体，注解上使用即时Wrapper
         *     （{@code @Wrapper}的{@code waitReqCreatComplete=false}），因此被注解的方法无需配置URL。
         * </pre>
         *
         * @param context 方法上下文
         * @return 与当前方法返回值类型匹配的结果
         */
        @FunctionAlias("__chunk_upload__")
        public static Object upload(MethodContext context) {

            ChunkUpload chunkUploadAnn = context.getMergedAnnotation(ChunkUpload.class);

            File file = resolveFile(context, chunkUploadAnn);
            String fileId = resolveFileId(context, chunkUploadAnn, file);
            String fileName = resolveFileName(context, chunkUploadAnn, file);
            ChunkUploadApi uploadApi = resolveUploadApi(context, chunkUploadAnn);

            ChunkUploadOptions options = new ChunkUploadOptions()
                    .setChunkSize(parseLongExpression(context, chunkUploadAnn.chunkSize(),
                            ChunkUploadOptions.DEFAULT_CHUNK_SIZE, "chunkSize"))
                    .setMaxConcurrency(parseIntExpression(context, chunkUploadAnn.maxConcurrency(),
                            ChunkUploadOptions.DEFAULT_MAX_CONCURRENCY, "maxConcurrency"))
                    .setMaxRetryCount(parseIntExpression(context, chunkUploadAnn.maxRetryCount(),
                            ChunkUploadOptions.DEFAULT_MAX_RETRY_COUNT, "maxRetryCount"))
                    .setRetryBackoffMillis(parseLongExpression(context, chunkUploadAnn.retryBackoffMillis(),
                            ChunkUploadOptions.DEFAULT_RETRY_BACKOFF_MILLIS, "retryBackoffMillis"))
                    .setSkipUploaded(chunkUploadAnn.skipUploaded())
                    .setMergeAfterSuccess(chunkUploadAnn.mergeAfterSuccess())
                    .setDeleteOnMergeFailure(chunkUploadAnn.deleteOnMergeFailure());
            options.validate();

            log.info("Start chunk upload for file[{}], fileId={}, chunk size={} bytes",
                    file.getAbsolutePath(), fileId, options.getChunkSize());
            ChunkUploadResult result = new ChunkUploadExecutor(uploadApi, options, context.getAsyncTaskExecutor())
                    .upload(file, fileId, fileName);
            log.info("Chunk upload of file[{}] finished: {}", file.getAbsolutePath(), result);

            return convertReturnValue(context, result);
        }

        /**
         * 解析数值型表达式属性（long维度）
         * <pre>
         *     与{@code Retryable#waitMillis}等属性保持同一套路：属性声明为String，
         *     字面量与SpEL都交给{@link MethodContext#parseExpression}求解；
         *         留空时不回退默认值而是直接用{@link ChunkUploadOptions}的默认配置，
         *         避免注解与配置两处各写一份默认值。
         * </pre>
         *
         * @param context      方法上下文
         * @param expression   注解上声明的表达式，可为空串
         * @param defaultValue 留空时使用的默认值
         * @param attributeName 属性名，用于错误提示
         * @return 解析后的数值
         */
        private static long parseLongExpression(MethodContext context, String expression, long defaultValue, String attributeName) {
            if (!StringUtils.hasText(expression)) {
                return defaultValue;
            }
            Long value = context.parseExpression(expression, Long.class);
            if (value == null) {
                throw new ChunkUploadException("@ChunkUpload: the '{}' expression '{}' resolved to null.", attributeName, expression);
            }
            return value;
        }

        /**
         * 解析数值型表达式属性（int维度），规则见{@link #parseLongExpression}
         *
         * @param context      方法上下文
         * @param expression   注解上声明的表达式，可为空串
         * @param defaultValue 留空时使用的默认值
         * @param attributeName 属性名，用于错误提示
         * @return 解析后的数值
         */
        private static int parseIntExpression(MethodContext context, String expression, int defaultValue, String attributeName) {
            if (!StringUtils.hasText(expression)) {
                return defaultValue;
            }
            Integer value = context.parseExpression(expression, Integer.class);
            if (value == null) {
                throw new ChunkUploadException("@ChunkUpload: the '{}' expression '{}' resolved to null.", attributeName, expression);
            }
            return value;
        }

        /**
         * 解析待上传的文件
         * <pre>
         *     只支持{@link File}与文件路径字符串两种结果，其他类型（如{@code MultipartFile}、{@code byte[]}）
         *     会给出明确的错误提示，由使用者先转成文件再上传——避免引入语义不清的隐式转换。
         * </pre>
         *
         * @param context 方法上下文
         * @param ann     当前注解
         * @return 待上传的文件
         */
        private static File resolveFile(MethodContext context, ChunkUpload ann) {

            String fileExpression = ann.file();
            Object fileValue;
            if (StringUtils.hasText(fileExpression)) {
                fileValue = context.parseExpression(fileExpression, Object.class);
                if (fileValue == null) {
                    throw new ChunkUploadException("@ChunkUpload: the 'file' expression '{}' resolved to null.", fileExpression);
                }
            } else {
                fileValue = context.getArgument(File.class);
                if (fileValue == null) {
                    throw new ChunkUploadException("@ChunkUpload: no file to upload, please configure the 'file' attribute "
                            + "or declare a java.io.File parameter on method '{}'.", context.getSimpleSignature());
                }
            }

            if (fileValue instanceof File) {
                return (File) fileValue;
            }
            if (fileValue instanceof String) {
                String path = (String) fileValue;
                if (!StringUtils.hasText(path)) {
                    throw new ChunkUploadException("@ChunkUpload: the 'file' attribute resolved to an empty path.");
                }
                return new File(path);
            }
            throw new ChunkUploadException("@ChunkUpload: the 'file' attribute only supports java.io.File and String, "
                    + "but got {}. Please convert it to a file first.", fileValue.getClass().getName());
        }

        /**
         * 解析文件唯一标识
         * <pre>
         *     注解未配置时以文件内容MD5作为标识，因此同一份文件的中断上传可以续传；
         *     计算MD5需要完整读一遍文件，超大文件建议在注解上直接给出业务标识避免这次读取。
         * </pre>
         *
         * @param context 方法上下文
         * @param ann     当前注解
         * @param file    待上传的文件
         * @return 文件唯一标识
         */
        private static String resolveFileId(MethodContext context, ChunkUpload ann, File file) {

            String fileIdExpression = ann.fileId();
            if (StringUtils.hasText(fileIdExpression)) {
                String fileId = context.parseExpression(fileIdExpression, String.class);
                if (StringUtils.hasText(fileId)) {
                    return fileId;
                }
            }
            try {
                return DigestFunctions.md5Hex(file);
            } catch (Exception e) {
                throw new ChunkUploadException(e, "@ChunkUpload: failed to compute the MD5 of file '{}' as fileId.", file.getAbsolutePath());
            }
        }

        /**
         * 解析服务端保存时使用的文件名
         *
         * @param context 方法上下文
         * @param ann     当前注解
         * @param file    待上传的文件
         * @return 文件名，未配置时使用原始文件名
         */
        private static String resolveFileName(MethodContext context, ChunkUpload ann, File file) {

            String fileNameExpression = ann.fileName();
            if (StringUtils.hasText(fileNameExpression)) {
                String fileName = context.parseExpression(fileNameExpression, String.class);
                if (StringUtils.hasText(fileName)) {
                    return fileName;
                }
            }
            return file.getName();
        }

        /**
         * 获取服务端协议接口实例
         *
         * @param context 方法上下文
         * @param ann     当前注解
         * @return 协议接口实例
         */
        private static ChunkUploadApi resolveUploadApi(MethodContext context, ChunkUpload ann) {

            Class<? extends ChunkUploadApi> apiClass = ann.api();
            if (apiClass != ChunkUploadApi.class) {
                return context.getHttpProxyFactory().getProxyObject(apiClass);
            }

            // 未指定协议接口时使用声明当前方法的接口，要求它本身继承了ChunkUploadApi
            Object proxyObject = context.getProxyObject();
            if (proxyObject instanceof ChunkUploadApi) {
                return (ChunkUploadApi) proxyObject;
            }
            throw new ChunkUploadException("@ChunkUpload: the interface declaring this method must extend ChunkUploadApi, "
                    + "otherwise specify the protocol interface with the 'api' attribute. Current interface: {}",
                    proxyObject.getClass().getName());
        }

        /**
         * 把上传结果转换成方法声明的返回值类型
         *
         * @param context 方法上下文
         * @param result  上传结果
         * @return 与返回值类型匹配的对象
         */
        private static Object convertReturnValue(MethodContext context, ChunkUploadResult result) {

            Class<?> returnType = context.getReturnResolvableType().toClass();
            if (void.class == returnType || Void.class == returnType) {
                return null;
            }
            if (returnType == ChunkUploadResult.class) {
                return result;
            }
            if (returnType == Boolean.class || returnType == boolean.class) {
                return result.isSuccess();
            }
            throw new ChunkUploadException("@ChunkUpload annotation unsupported method return value type: {}", returnType);
        }
    }
}
