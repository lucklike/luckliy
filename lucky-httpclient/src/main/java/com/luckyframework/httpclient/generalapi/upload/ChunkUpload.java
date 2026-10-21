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
 *     public interface FileUploadApi extends ChunkUploadApi<List<Long>> {
 *
 *         // 查询服务端已收到的分片：返回类型就是类型参数T（片号、分片ID皆可）
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
 *         // 通知服务端合并
 *         @Post("merge")
 *         boolean mergeChunks(@Query("fileId") String fileId,
 *                             @Query("fileName") String fileName,
 *                             @Query("totalChunks") int totalChunks);
 *
 *         // 声明式分片上传：协议方法在本接口上，无需再指定api
 *         @ChunkUpload(chunkSize = 1024 * 1024, maxRetryCount = 5)
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
 *     表达式属性（{@link #file()}/{@link #fileId()}/{@link #fileName()}）都支持SpEL，
 *     可引用方法参数、{@code #{$method_args$}}等上下文变量，详见{@link SpELVariableNote}。
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
     * 单个分片的字节数
     *
     * @return 单个分片的字节数
     */
    long chunkSize() default ChunkUploadOptions.DEFAULT_CHUNK_SIZE;

    /**
     * 同时上传的分片数量上限
     *
     * @return 最大并发数
     */
    int maxConcurrency() default ChunkUploadOptions.DEFAULT_MAX_CONCURRENCY;

    /**
     * 分片上传失败后的最大重传轮数
     *
     * @return 最大重传轮数，小于0表示无限重传直到全部分片上传成功
     */
    int maxRetryCount() default ChunkUploadOptions.DEFAULT_MAX_RETRY_COUNT;

    /**
     * 每轮重传前的等待时间（毫秒）
     *
     * @return 等待时间（毫秒）
     */
    long retryBackoffMillis() default ChunkUploadOptions.DEFAULT_RETRY_BACKOFF_MILLIS;

    /**
     * 是否在上传前查询并跳过服务端已有的分片
     * <pre>
     *     服务端没有提供查询接口时（{@link ChunkUploadApi#uploadedChunks(String)}保持默认实现）应设为false，
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
                    .setChunkSize(chunkUploadAnn.chunkSize())
                    .setMaxConcurrency(chunkUploadAnn.maxConcurrency())
                    .setMaxRetryCount(chunkUploadAnn.maxRetryCount())
                    .setRetryBackoffMillis(chunkUploadAnn.retryBackoffMillis())
                    .setSkipUploaded(chunkUploadAnn.skipUploaded())
                    .setMergeAfterSuccess(chunkUploadAnn.mergeAfterSuccess());
            options.validate();

            log.info("Start chunk upload for file[{}], fileId={}, chunk size={} bytes",
                    file.getAbsolutePath(), fileId, options.getChunkSize());
            ChunkUploadResult result = new ChunkUploadExecutor(uploadApi, options, context.getAsyncTaskExecutor())
                    .upload(file, fileId, fileName);
            log.info("Chunk upload of file[{}] finished: {}", file.getAbsolutePath(), result);

            return convertReturnValue(context, result);
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
