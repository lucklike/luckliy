package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.annotations.Binary;
import com.luckyframework.httpclient.proxy.annotations.MultipartFormData;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MultipartForm静态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/3 00:26
 */
public class MultipartFormStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        MultipartFormData mfdAnn = context.toAnnotation(MultipartFormData.class);

        // condition条件检验
        String condition = mfdAnn.condition();
        if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
            return Collections.emptyList();
        }

        List<ParamInfo> paramInfos = new ArrayList<>();

        // Key-Value分隔符
        String separation = mfdAnn.separator();

        // 添加Txt格式的数据
        IfExpressionUtils.filterAndAdd(
                context.getContext(),
                paramInfos,
                mfdAnn.txt(),
                separation,
                (e, k, v, kv, vv) -> new ParamInfo(kv, String.valueOf(vv))
        );

        // 添加文件格式的数据
        IfExpressionUtils.filterAndAdd(
                context.getContext(),
                paramInfos,
                mfdAnn.file(),
                separation,
                (e, k, v, kv, vv) -> {
                    if (HttpExecutor.isResourceParam(vv)) {
                        return new ParamInfo(kv, vv);
                    }
                    throw new IllegalArgumentException(StringUtils.format("The value '{}' corresponding to the key named '{}' in the Map cannot be converted to HttpFile type.", ClassUtils.getClassSimpleName(vv), kv));
                }
        );

        // 添加二进制格式的数据
        for (Binary binary : mfdAnn.binary()) {
            String fileCondition = binary.condition();
            if (StringUtils.hasText(fileCondition) && !context.parseExpression(fileCondition, boolean.class)) {
                continue;
            }

            String name = context.parseExpression(binary.name(), String.class);
            String filename = context.parseExpression(binary.filename(), String.class);
            Object fileValue = context.parseExpression(binary.file());

            // 是否为可用的二进制内容由HttpExecutor.isBinaryParam统一判定，非二进制类型直接拒绝，
            // 命中后由toHttpFileWithFilename按具体形态包装成携带文件名的HttpFile
            if (!HttpExecutor.isBinaryParam(fileValue)) {
                throw new IllegalArgumentException(StringUtils.format(
                        "The value '{}' in @MultipartFormData.binary is not a binary type and cannot be used as a file part.",
                        ClassUtils.getClassSimpleName(fileValue)));
            }
            paramInfos.add(new ParamInfo(name, toHttpFileWithFilename(fileValue, filename)));
        }

        return paramInfos;
    }

    /**
     * 将 {@link HttpExecutor#isBinaryParam(Object)} 判定通过的二进制参数转换为携带自定义文件名的
     * {@link HttpFile} 实例。
     * <pre>
     *     资源系类型（File/Resource/MultipartFile/HttpFile）走各自的构造器，保留流式读取；
     *     InputStream按流包装；byte[]/Byte[]/ByteBuffer/Reader等其余二进制形态
     *     统一经{@link HttpExecutor#toByte(Object)}转成字节后包装。
     * </pre>
     *
     * @param param    二进制参数对象，支持 {@link HttpFile}、{@link File}、{@link Resource}、
     *                 {@link MultipartFile}、{@link InputStream}、{@link InputStreamSource}、
     *                 {@code byte[]}、{@code Byte[]}、{@code ByteBuffer}、{@code Reader}
     * @param filename 自定义文件名，将覆盖内容自带的文件名
     * @return 携带指定文件名的 HttpFile 实例
     */
    private HttpFile toHttpFileWithFilename(Object param, String filename) {
        if (param instanceof HttpFile) {
            HttpFile hf = (HttpFile) param;
            return new HttpFile(hf, () -> filename, hf.getDescriptor());
        }
        if (param instanceof File) {
            return new HttpFile((File) param, filename);
        }
        if (param instanceof Resource) {
            return new HttpFile((Resource) param, filename);
        }
        if (param instanceof MultipartFile) {
            return new HttpFile((MultipartFile) param, filename);
        }
        if (param instanceof InputStream) {
            return new HttpFile((InputStream) param, filename);
        }
        if (param instanceof InputStreamSource) {
            return new HttpFile((InputStreamSource) param, () -> filename, "[InputStreamSource] " + filename);
        }
        // byte[]/Byte[]/ByteBuffer/Reader：统一转成字节后包装
        return new HttpFile(HttpExecutor.toByte(param), filename);
    }
}
