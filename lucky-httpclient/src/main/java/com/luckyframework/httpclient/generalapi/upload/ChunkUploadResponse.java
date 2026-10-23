package com.luckyframework.httpclient.generalapi.upload;

/**
 * 分片上传协议响应的统一载体。
 * <pre>
 *     {@link ChunkUploadApi#mergeChunks}与{@link ChunkUploadApi#instantUpload}的返回类型。
 *     协议接口只用{@code boolean}回报成败，承载不了服务端的附加信息——不少系统在合并完成后
 *     会返回一个内部文件ID或下载URL，秒传命中时同样如此，因此用本对象同时携带：
 *     1.{@code success}：框架唯一判读的字段，决定合并/秒传是否视为成功；
 *     2.{@code data}：服务端返回的原始载荷（内部文件ID、下载URL、元数据JSON等），
 *       框架不解析其内容，原样透传到{@link ChunkUploadResult#getServerData()}供调用方取用。
 *
 *     用注解声明协议时有两种接法：
 *     {@code
 *     // 接法1：服务端直接返回与本类字段同名的JSON对象，交给框架自动转换
 *     @Post("merge")
 *     @RespConvert("#{$body$.data}")
 *     ChunkUploadResponse mergeRequest(@Query("fileId") String fileId, ...);
 *     // 服务端返回 {"code":200,"data":{"success":true,"data":"srv-file-001"}} 即自动绑定
 *
 *     // 接法2：响应结构不匹配时，先拿原始结果再手工包装
 *     default ChunkUploadResponse mergeChunks(String fileId, File file, int totalChunks) {
 *         String serverFileId = mergeRequest(fileId, file.getName(), totalChunks);
 *         return serverFileId != null ? ChunkUploadResponse.success(serverFileId)
 *                                     : ChunkUploadResponse.failed();
 *     }
 *     }
 *
 *     本类同时提供无参构造与setter，满足框架Map到POJO的自动转换要求；
 *     业务代码建议使用静态工厂方法创建实例。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-23 09:30
 * @see ChunkUploadApi
 * @see ChunkUploadResult#getServerData()
 */
public class ChunkUploadResponse {

    /**
     * 协议动作是否执行成功，框架唯一判读的字段
     */
    private boolean success;

    /**
     * 服务端返回的原始载荷，如内部文件ID、下载URL、元数据JSON等，框架不解析
     */
    private Object data;

    /**
     * 构造成功响应，不携带数据
     *
     * @return success为true的响应
     */
    public static ChunkUploadResponse success() {
        return new ChunkUploadResponse(true, null);
    }

    /**
     * 构造成功响应并携带服务端数据
     *
     * @param data 服务端返回的原始载荷，内部文件ID、下载URL等
     * @return success为true的响应
     */
    public static ChunkUploadResponse success(Object data) {
        return new ChunkUploadResponse(true, data);
    }

    /**
     * 构造失败响应
     *
     * @return success为false的响应
     */
    public static ChunkUploadResponse failed() {
        return new ChunkUploadResponse(false, null);
    }

    /**
     * 无参构造，供框架将JSON响应自动转换为本类实例使用
     */
    public ChunkUploadResponse() {
    }

    /**
     * 构造协议响应
     *
     * @param success 协议动作是否执行成功
     * @param data    服务端返回的原始载荷，可为null
     */
    public ChunkUploadResponse(boolean success, Object data) {
        this.success = success;
        this.data = data;
    }

    /**
     * 获取协议动作的执行成败
     *
     * @return 成功返回true
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置协议动作的执行成败
     *
     * @param success 是否执行成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取服务端返回的原始载荷
     * <pre>
     *     框架不解析本值内容，返回的可能是内部文件ID、下载URL、元数据JSON等，
     *     具体含义由服务端协议与调用方约定。
     * </pre>
     *
     * @return 原始载荷，服务端未返回附加信息时为null
     */
    public Object getData() {
        return data;
    }

    /**
     * 设置服务端返回的原始载荷
     *
     * @param data 原始载荷
     */
    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ChunkUploadResponse{" +
                "success=" + success +
                ", data=" + data +
                '}';
    }
}
