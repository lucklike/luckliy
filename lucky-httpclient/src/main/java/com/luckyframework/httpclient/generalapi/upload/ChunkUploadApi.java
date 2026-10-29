package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

/**
 * 分片上传的服务端协议接口。
 * <pre>
 *     HTTP分片上传没有像下载（{@code Range}请求头）那样的标准协议，不同服务端的
 *     「申请文件标识、秒传探测、判断分片是否已上传、上传单个分片、通知合并、删除分片、释放资源」各接口的
 *     URL、参数名、响应格式都不一样，因此本接口只约定<b>动作</b>，具体协议由使用者用自己的API接口去实现。
 *     「上传单个分片」「通知合并」「删除分片」「秒传探测」四个动作都没有可兜底的协议假设，
 *     默认实现直接抛{@link ChunkUploadNotImplementedException}；其中「上传单个分片」「通知合并」<b>必须</b>覆写，
 *     「删除分片」只在开启「合并失败时删除分片」（{@link ChunkUploadOptions#isDeleteOnMergeFailure()}）时才被调用，
 *     「秒传探测」只在开启{@link ChunkUploadOptions#isInstantUpload()}时才被调用；
 *     两个可选动作不开启就无需覆写，开启而未覆写时，该异常会被内核记录进{@link ChunkUploadResult}与日志，不会向外抛；
 *     「申请文件标识」（{@link #initUpload}）只在调用方未显式给出fileId时才被调用，
 *     默认返回{@code null}表示服务端不生成，内核改用文件内容MD5兜底，因此不需要服务端生成标识时无需覆写；
 *     「判断分片是否已上传」默认返回{@code false}，即服务端没有查询能力时不做跳过、全量上传，
 *     需要断点续传时覆写该方法，按真实查询协议对账即可；
 *     「释放资源」（{@link #releaseResources}）默认空实现，每次上传结束后无论成败都会被调用一次，
 *     没有需要释放的资源时无需覆写。
 *
 *     实现时只需把下面几个方法接到真实接口上，例如：
 *     {@code
 *     @ServerAddress("http://localhost:8864/shard/upload/")
 *     public interface FileChunkUploadApi extends ChunkUploadApi {
 *
 *         // 0.申请文件标识：服务端生成任务ID/文件ID时覆写本方法，参数统一由ChunkUploadMeta提供；
 *         //   返回null或空串表示服务端不生成，内核会退回用文件内容MD5兜底
 *         @Override
 *         default String initUpload(ChunkUploadMeta meta) {
 *             return createTask(meta.getFileName(), meta.getTotalSize(), meta.getTotalChunks());
 *         }
 *
 *         @Post("task/create")
 *         @RespConvert("#{$body$.data.taskId}")
 *         String createTask(@Query("fileName") String fileName,
 *                           @Query("size") long totalSize,
 *                           @Query("chunks") int totalChunks);
 *
 *         // 1.秒传探测：切片之前先问服务端「这个文件是否已存在」，data可携带服务端文件ID；
 *         //   探测参数全部从ChunkUploadMeta取，内容摘要需要时用DigestFunctions自行计算；
 *         //   只在开启instantUpload时被调用，不开启无需覆写
 *         @Override
 *         default ChunkUploadResponse checkInstantUpload(ChunkUploadMeta meta) {
 *             return existsRequest(meta.getFileId(), meta.getFileName(), meta.getTotalSize());
 *         }
 *
 *         @Get("instant")
 *         @RespConvert("#{$body$.data}")
 *         ChunkUploadResponse existsRequest(@Query("fileId") String fileId,
 *                                           @Query("fileName") String fileName,
 *                                           @Query("totalSize") long totalSize);
 *
 *         // 2.判断分片是否已上传：先查服务端已有的片号集合，再与当前分片对账；
 *         //   本方法会被每个分片调用一次，片数多时在覆写实现里缓存查询结果或转成Set再判断
 *         @Override
 *         default boolean isChunkUploaded(ChunkUploadMeta meta, FileChunk fileChunk) {
 *             return uploadedChunks(meta.getFileId()).contains(fileChunk.getIndex());
 *         }
 *
 *         @Get("uploadChunks")
 *         @RespConvert("#{$body$.data}")
 *         List<Long> uploadedChunks(@Query("fileId") String fileId);
 *
 *         // 3.上传单个分片：本接口给的是FileChunk对象，表单协议要的是拆开的字段，
 *         //   在覆写实现里拆解即可；接口代理会直接执行default方法体，
 *         //   方法体内的调用仍然会发出真实的HTTP请求
 *         @Override
 *         default boolean uploadChunk(ChunkUploadMeta meta, FileChunk fileChunk) {
 *             return uploadChunkForm(meta.getFileId(), fileChunk.getIndex(), fileChunk.getContent());
 *         }
 *
 *         @Post("chunk")
 *         boolean uploadChunkForm(@MultiData("fileId") String fileId,
 *                                 @MultiData("chunkNumber") long chunkNumber,
 *                                 @MultiFile(value = "file", fileName = "chunk") byte[] content);
 *
 *         // 4.通知服务端合并：文件名、总大小、分片总数与原始文件都在ChunkUploadMeta里，按协议取用；
 *         //   分片清单在ChunkMergeContext里，需要上报片号列表或逐片MD5时从那里取；
 *         //   合并成功后服务端若返回内部文件ID/下载URL，放进ChunkUploadResponse的data原样带回
 *         @Override
 *         default ChunkUploadResponse mergeChunks(ChunkUploadMeta meta, ChunkMergeContext context) {
 *             return mergeRequest(meta.getFileId(), meta.getFileName(),
 *                     meta.getTotalSize(), meta.getTotalChunks());
 *         }
 *
 *         @Post("merge")
 *         @RespConvert("#{$body$.data}")
 *         ChunkUploadResponse mergeRequest(@Query("fileId") String fileId,
 *                                          @Query("fileName") String fileName,
 *                                          @Query("totalSize") long totalSize,
 *                                          @Query("totalChunks") int totalChunks);
 *
 *         // 5.删除服务端已收到的分片：可选动作，只在开启「合并失败时删除分片」且合并
 *         //   实际发起仍失败后被调用；不启用该开关就不用覆写，启用则必须覆写，
 *         //   否则默认实现抛ChunkUploadNotImplementedException
 *         @Override
 *         default boolean deleteChunks(ChunkUploadMeta meta) {
 *             return deleteRequest(meta.getFileId());
 *         }
 *
 *         @Delete("chunks")
 *         @RespConvert("#{$body$.data}")
 *         boolean deleteRequest(@Query("fileId") String fileId);
 *
 *         // 6.释放资源：上传结束后无论成败都会被调用一次，且在所有协议动作之后；
 *         //   默认空实现，没有需要释放的资源时不用覆写
 *         @Override
 *         default void releaseResources(ChunkUploadMeta meta, ChunkMergeContext context) {
 *             releaseSession(meta.getFileId());
 *         }
 *
 *         @Post("release")
 *         void releaseSession(@Query("fileId") String fileId);
 *     }
 *     }
 *
 *     需要上报哪些参数由服务端协议决定：除了{@code fileChunk.getIndex()}与{@code fileChunk.getContent()}，
 *     {@link FileChunk}还提供{@code getStart()/getEnd()/getSize()}与
 *     {@code getTotalSize()/getChunkSize()/getTotalChunks()}，需要把区间或切分元数据一并上报时直接取用；
 *     分片内容也可以改用{@code fileChunk.writeContentTo(out)}流式写出，避免整片驻留内存；
 *     multipart表单要求文件项带文件名时，用{@code fileChunk.toHttpFile("blob")}把整片
 *     包装成{@code @MultiFile}可直接接收的{@code HttpFile}。
 *     「整个文件」范围的数据由{@link ChunkUploadMeta}提供同名方法：{@code meta.md5()/sha256()/base64()}
 *     取整文件指纹（流式计算，不把文件读进内存）、{@code meta.writeContentTo(out)}流式写出整个文件、
 *     {@code meta.toHttpFile()}把整个文件包装成{@code HttpFile}——与{@link FileChunk}的同名方法一一对应，
 *     区别只在作用范围，别拿分片摘要当文件摘要用。
 *
 *     实现时的八条约定：
 *     1.各方法内部可以正常使用本框架的注解（{@code @Post}/{@code @MultiFile}/{@code @Retryable}等），
 *       它们会各自发出真实的HTTP请求；
 *     2.七个协议方法都声明了{@code throws Exception}，实现里可以直接抛检查异常（如读写本地文件、
 *       调用第三方SDK），不必自行包装成运行时异常；返回值用{@code boolean}表达业务成功与否即可，
 *       抛异常会被框架记录为分片执行失败并纳入重试，返回{@code false}同样会被视为需要重试的分片；
 *     3.声明为{@code boolean}的方法要求服务端返回能被自动转成布尔值的响应体，
 *       {@code text/plain}会被框架原样当字符串返回而无法转成{@code boolean}，
 *       这类接口请返回{@code application/json}（如响应体直接是{@code true}），
 *       或者用{@code @RespConvert}从JSON包装结果里取出布尔值；
 *     4.框架不假定服务端用片号标识分片：{@code isChunkUploaded}的覆写实现决定怎么对账，
 *       片号之外的标识（分片ID、内容MD5等）覆写该方法、从{@code fileChunk}
 *       （索引/区间/大小/内容）取值比对即可；
 *     5.{@code deleteChunks}是唯一的「失败清理」动作，只在合并实际发起且仍失败后被调用；
 *       分片没传完而未发起合并时不会调用它——那些分片留着下次续传还有用；
 *       与{@code uploadChunk}/{@code mergeChunks}一样没有可兜底的协议假设，默认实现抛
 *       {@link ChunkUploadNotImplementedException}：不启用删除开关就无需覆写，启用则必须覆写。
 *     6.「合并」「秒传」两个动作返回{@link ChunkUploadResponse}而非{@code boolean}：
 *       框架只判读{@code isSuccess()}，{@code getData()}是服务端附加信息的透传通道
 *       （内部文件ID、下载URL等），原样落进{@link ChunkUploadResult#getServerData()}，
 *       没有附加信息时返回{@code ChunkUploadResponse.success()}即可。
 *     7.文件级协议参数统一由{@link ChunkUploadMeta}提供（原始文件、fileId、文件名、总大小、
 *       单片大小、分片总数），不必再从{@code File}反推切分依据；除{@link #initUpload}外，
 *       {@link #checkInstantUpload}、{@link #isChunkUploaded}、{@link #uploadChunk}、
 *       {@link #mergeChunks}、{@link #deleteChunks}与{@link #releaseResources}收到的是同一个{@code meta}
 *       （此时fileId已确定），协议要报哪些参数就取哪些；分片级参数仍在{@link FileChunk}上，
 *       而{@link #mergeChunks}与{@link #releaseResources}还会额外收到{@link ChunkMergeContext}——
 *       本次切出的全部分片与本次实际上传成功的分片，需要上报片号列表、逐片MD5等
 *       「分片清单」类参数时从那里取（{@link #releaseResources}的特殊情形见其方法注释）。
 *     8.fileId的来源按优先级为：调用方显式指定（{@link ChunkUploadExecutor}的方法参数，
 *       或注解{@code fileId}属性）→ 服务端生成（覆写{@link #initUpload}）→ 文件内容MD5；
 *       前两级都没有时才进入下一级，服务端生成抛异常时本次上传直接以失败结束、
 *       不发起任何分片请求，原因见{@link ChunkUploadResult#getInitError()}。
 * </pre>
 *
 * @author fukang
 * @version 4.0.0
 * @date 2026-10-21 10:45
 * @see ChunkUploadExecutor
 * @see ChunkUploadMeta
 * @see ChunkMergeContext
 * @see ChunkUpload
 */
public interface ChunkUploadApi {

    /**
     * 申请文件唯一标识：由服务端生成任务ID/文件ID的扩展点
     * <pre>
     *     只在调用方没有显式给出fileId时被调用，且发生在切片、秒传探测之前；
     *     参数里已经给出文件名、总大小、单片大小与分片总数，足以发起多数「创建上传任务」的请求；
     *     此时{@link ChunkUploadMeta#getFileId()}为{@code null}——要申请的正是这个标识。
     *     返回{@code null}或空串表示服务端不生成标识（默认实现即如此），
     *     内核改用文件内容MD5作为fileId，因此不需要服务端生成时无需覆写。
     *     抛出的异常不会向外传播：本次上传直接以失败结束，不发起任何分片请求，
     *     原因记录在{@link ChunkUploadResult#getInitError()}；
     *     秒传是优化手段、失败可以降级，而本方法属于必要准备，失败就只能整体失败。
     *     服务端返回的标识会贯穿后续的{@code checkInstantUpload}、{@code isChunkUploaded}、
     *     {@code uploadChunk}、{@code mergeChunks}与{@code deleteChunks}，
     *     同时记录在{@link ChunkUploadResult#getFileId()}供调用方取用（例如拼下载地址）。
     * </pre>
     *
     * @param meta 文件级元信息，此时{@code fileId}为null，其余字段均已确定
     * @return 服务端生成的文件唯一标识；返回null或空串表示改用文件内容MD5兜底
     */
    default String initUpload(ChunkUploadMeta meta) throws Exception {
        return null;
    }

    /**
     * 秒传探测：上传前询问服务端该文件是否已存在
     * <pre>
     *     本方法只做「查询」，不产生任何上传或跳过效果：命中（{@code isSuccess()}为true）后
     *     跳过全部分片才是内核的行为。命名与{@link ChunkUploadOptions#isInstantUpload()}
     *     这个开关刻意区分开——一个是「要不要探测」，一个是「发起一次探测」。
     *     只在开启{@link ChunkUploadOptions#isInstantUpload()}时被调用，且发生在
     *     {@code ChunkUtils.shard}切片<b>之前</b>——命中即返回，全部分片的对账、上传与合并都不再执行。
     *     探测哪些信息（内容MD5、大小、文件名）由协议实现自己从{@link ChunkUploadMeta}取用，
     *     框架不假定服务端用什么标识整个文件；也不预计算内容摘要——不开启秒传时白读一遍文件
     *     得不偿失，需要摘要时用{@code meta.md5()}或{@code meta.sha256()}
     *     （按流读取，内存占用与文件大小无关）计算并自行缓存；
     *     命中时（{@code isSuccess()}为{@code true}）若服务端返回内部文件ID/下载URL，
     *     放进{@code data}透传到{@link ChunkUploadResult#getServerData()}。
     *     未命中或探测失败（返回{@code false}、{@code null}或抛异常）都不会中断本次上传，
     *     内核只记录warn日志，继续走正常分片流程；
     *     与{@code uploadChunk}/{@code mergeChunks}一样没有可兜底的默认协议，
     *     启用秒传开关时必须由子类或子接口覆写，未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}并被降级为未命中。
     * </pre>
     *
     * @param meta 文件级元信息，含fileId（此时一定已确定）、文件名、总大小、单片大小、
     *             分片总数与原始文件
     * @return 秒传响应；{@code isSuccess()}为{@code true}表示服务端已有该文件，本次无需上传
     */
    default ChunkUploadResponse checkInstantUpload(ChunkUploadMeta meta) throws Exception {
        throw new ChunkUploadNotImplementedException("check whether the server already has this whole file (instant upload)",
                "checkInstantUpload(ChunkUploadMeta)",
                "the instant-upload check can never hit and every byte will be uploaded again");
    }

    /**
     * 当前分片服务端是否已经收到，即「分片用什么标识」的扩展点
     * <pre>
     *     用于断点续传：命中的分片会在上传前被跳过，不再重复传输。
     *     本方法对每个分片各调用一次，服务端协议是「一次查询全部片号」时，
     *     在覆写实现里自己完成查询与比对（{@code fileChunk.getIndex()}即片号），
     *     片数多且报文量大时建议先转成Set再判断，避免逐片远程查询。
     *     默认返回{@code false}，即不覆写就等价于「服务端没有查询接口」，每次全量上传。
     *     收到的{@code meta}与{@link #uploadChunk}、{@link #mergeChunks}是同一个对象，
     *     文件标识就是{@code meta.getFileId()}。
     * </pre>
     *
     * @param meta      文件级元信息，fileId此时一定已确定；需要文件名、大小等参数时直接取用
     * @param fileChunk 待判断的分片，可从中取得索引、区间、大小乃至内容用于比对
     * @return true表示服务端已有该分片，本次可以跳过
     */
    default boolean isChunkUploaded(ChunkUploadMeta meta, FileChunk fileChunk) throws Exception {
        return false;
    }

    /**
     * 上传单个分片
     * <pre>
     *     没有可兜底的默认协议，必须由子类或子接口覆写；未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}，该异常会走分片失败重试通道被原样上报。
     * </pre>
     *
     * @param meta      文件级元信息，fileId此时一定已确定；协议要报哪些参数就从这里取
     * @param fileChunk 待上传的分片，可通过它取得索引、区间、本片内容以及
     *                  {@code totalSize/chunkSize/totalChunks}等协议参数
     * @return 该分片是否上传成功；返回{@code false}或抛出异常都会让框架在下一轮重试该分片
     */
    default boolean uploadChunk(ChunkUploadMeta meta, FileChunk fileChunk) throws Exception {
        throw new ChunkUploadNotImplementedException("upload a single chunk", "uploadChunk(ChunkUploadMeta, FileChunk)",
                "no chunk can reach the server and the file will never be uploaded completely");
    }

    /**
     * 通知服务端合并全部分片
     * <pre>
     *     没有可兜底的默认协议，必须由子类或子接口覆写；未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}，由框架记录为合并失败。
     *     只有在所有分片都上传成功之后才会被调用（由{@link ChunkUploadOptions#isMergeAfterSuccess()}控制），
     *     因此本方法无需再自行校验分片完整性。
     *     文件名、总大小、分片总数与原始文件都在{@link ChunkUploadMeta}里，协议要报哪些就取哪些；
     *     需要上报「分片清单」类参数（片号列表、逐片内容MD5、每片大小等）时从{@link ChunkMergeContext}取：
     *     {@code getAllChunks()}是本次切出的全部分片（含断点续传中被跳过的），
     *     {@code getUploadedChunks()}是本次实际上传成功的分片。
     *     返回值用{@link ChunkUploadResponse}表达成败；服务端合并后若返回内部文件ID、下载URL等附加信息，
     *     放进{@code data}原样带回即可，框架只判读{@code isSuccess()}，
     *     {@code data}会透传到{@link ChunkUploadResult#getServerData()}供调用方取用。
     * </pre>
     *
     * @param meta    文件级元信息，含fileId、文件名、总大小、单片大小、分片总数与原始文件；
     *                空文件的分片总数为0
     * @param context 分片清单，含本次切出的全部分片与本次实际上传成功的分片，两者均按片号升序
     * @return 合并响应；{@code isSuccess()}为{@code false}或抛出异常都会让框架记录合并失败
     */
    default ChunkUploadResponse mergeChunks(ChunkUploadMeta meta, ChunkMergeContext context) throws Exception {
        throw new ChunkUploadNotImplementedException("notify the server to merge all chunks", "mergeChunks(ChunkUploadMeta, ChunkMergeContext)",
                "the server will never produce the merged file");
    }

    /**
     * 删除服务端已收到的该文件的全部分片
     * <pre>
     *     可选的失败清理动作：只有开启{@link ChunkUploadOptions#isDeleteOnMergeFailure()}、
     *     且合并请求<b>实际发起</b>仍失败时才会被调用，用于回收合并失败后留在服务端的孤儿分片；
     *     分片没传完而未发起合并时不会调用——那些分片下次续传还要用。
     *     没有可兜底的默认协议，启用删除开关时必须由子类或子接口覆写；未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}，该异常由内核记录进{@link ChunkUploadResult}
     *     与日志（{@code getDeleteError()}），不会向外抛。
     * </pre>
     *
     * @param meta 文件级元信息，fileId此时一定已确定；与{@link #mergeChunks}收到的是同一个对象
     * @return 是否删除成功
     */
    default boolean deleteChunks(ChunkUploadMeta meta) throws Exception {
        throw new ChunkUploadNotImplementedException("delete the uploaded chunks on the server", "deleteChunks(ChunkUploadMeta)",
                "the orphan chunks left after a failed merge can never be reclaimed");
    }

    /**
     * 释放本次上传占用的资源：上传流程结束时的收尾动作
     * <pre>
     *     一次上传流程一旦真的开始（参数校验通过、进入文件标识解析），无论结果如何本方法都会被调用一次：
     *     正常上传完成、秒传命中、分片重试用尽、申请文件标识失败、合并失败，乃至流程中途抛出异常，
     *     内核都用{@code try/finally}走到这里，
     *     因此适合放「必须成对出现」的收尾逻辑——释放服务端会话或临时上传任务、
     *     关闭自建的客户端与连接、删除本地临时文件等。
     *     调用时机在所有协议动作之后（{@link #mergeChunks}与按需的{@link #deleteChunks}之后），
     *     此时分片已落定、合并已尝试完毕，不会再有请求被发起。
     *     默认实现什么也不做：服务端不需要额外释放动作时无需覆写。
     *     入参与其它动作同源：{@code meta}与{@link #mergeChunks}/{@link #deleteChunks}收到的是同一个对象，
     *     {@code context}的分片清单也与合并时一致。两种情形下信息不完整，协议需自行容错：
     *     1.{@link #initUpload}失败时本次上传未进入分片流程，{@code meta.getFileId()}为{@code null}，
     *       且{@code context}为空清单；
     *     2.秒传命中或文件未产生任何分片时，{@code meta}的分片总数为0，{@code context}为空清单。
     *     本方法抛出的异常会被内核捕获并写入error日志，既不会覆盖上传结果，
     *     也不会替换调用方原本会收到的异常，因此可以放心地做「尽力而为」的清理。
     * </pre>
     *
     * @param meta    文件级元信息，与本次流程中其它动作收到的是同一个对象；
     *                {@link #initUpload}失败时为{@code fileId}为null的那一份
     * @param context 本次切出的分片清单，与合并时一致；没有切片时为空清单
     */
    default void releaseResources(ChunkUploadMeta meta, ChunkMergeContext context) throws Exception {
        // 默认没有需要释放的资源
    }

    /**
     * 构造「成功」的协议响应，供SpEL表达式直接调用
     * <pre>
     *     等价于{@link ChunkUploadResponse#success(Object)}，这里提供一个短名是为了在
     *     {@code @RespConvert}等表达式里省去类名，同时把语义限定为「本次协议动作成功」：
     *     {@code
     *     @Post("merge")
     *     @RespConvert("#{chunk_success($body$.data)}")
     *     ChunkUploadResponse mergeRequest(@Query("fileId") String fileId, ...);
     *     }
     *     只表达成败；{@code data}的透传规则见{@link ChunkUploadResponse}。
     * </pre>
     *
     * @param serverData 服务端返回的原始载荷（内部文件ID、下载URL等），可以省略；
     *                   声明为可变参数只是为了允许不传，传入多个时只取第一个
     * @return success为true的响应
     * @see ChunkUploadResponse#success(Object)
     */
    @FunctionAlias("chunk_success")
    static ChunkUploadResponse success(Object... serverData) {
        if (ContainerUtils.isEmptyArray(serverData)) {
            return ChunkUploadResponse.success();
        }
        return ChunkUploadResponse.success(serverData[0]);
    }

    /**
     * 构造「失败」的协议响应，供SpEL表达式直接调用
     * <pre>
     *     等价于{@link ChunkUploadResponse#failed()}，与{@link #success}成对使用：
     *     {@code
     *     @Post("merge")
     *     @RespConvert("#{$body$.code == 200 ? chunk_success($body$.data) : chunk_failed()}")
     *     ChunkUploadResponse mergeRequest(@Query("fileId") String fileId, ...);
     *     }
     *     返回本响应与抛出异常等价，都会被框架判定为该协议动作失败。
     * </pre>
     *
     * @return success为false的响应
     * @see ChunkUploadResponse#failed()
     */
    @FunctionAlias("chunk_failed")
    static ChunkUploadResponse failed() {
        return ChunkUploadResponse.failed();
    }
}
