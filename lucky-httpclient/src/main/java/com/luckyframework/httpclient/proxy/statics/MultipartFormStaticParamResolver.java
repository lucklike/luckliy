package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.annotations.Binary;
import com.luckyframework.httpclient.proxy.annotations.File;
import com.luckyframework.httpclient.proxy.annotations.MultipartFormData;
import com.luckyframework.httpclient.proxy.annotations.Txt;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

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

        // 添加文本表单项
        for (Txt txtAnn : mfdAnn.txt()) {
            if (!isConditionMatched(context, txtAnn.condition())) {
                continue;
            }
            String name = context.parseExpression(txtAnn.name(), String.class);
            Object value = context.parseExpression(txtAnn.value());
            paramInfos.add(new ParamInfo(name, String.valueOf(value)));
        }

        // 添加文件表单项
        for (File fileAnn : mfdAnn.file()) {
            if (!isConditionMatched(context, fileAnn.condition())) {
                continue;
            }
            String name = context.parseExpression(fileAnn.name(), String.class);
            Object fileValue = context.parseExpression(fileAnn.file());

            // 是否为可用的资源类型由HttpExecutor.isResourceParam统一判定
            if (!HttpExecutor.isResourceParam(fileValue)) {
                throw new IllegalArgumentException(StringUtils.format(
                        "The value '{}' in @MultipartFormData.file is not a resource type and cannot be used as a file part.",
                        ClassUtils.getClassSimpleName(fileValue)));
            }
            paramInfos.add(new ParamInfo(name, toHttpFiles(
                    fileValue,
                    context.parseExpression(fileAnn.filename(), String.class),
                    context.parseExpression(fileAnn.contentType(), String.class))));
        }

        // 添加二进制表单项
        for (Binary binaryAnn : mfdAnn.binary()) {
            if (!isConditionMatched(context, binaryAnn.condition())) {
                continue;
            }
            String name = context.parseExpression(binaryAnn.name(), String.class);
            Object fileValue = context.parseExpression(binaryAnn.file());

            // 是否为可用的二进制内容由HttpExecutor.isBinaryParam统一判定，非二进制类型直接拒绝，
            // 命中后由toHttpFile按具体形态包装成携带文件名的HttpFile
            if (!HttpExecutor.isBinaryParam(fileValue)) {
                throw new IllegalArgumentException(StringUtils.format(
                        "The value '{}' in @MultipartFormData.binary is not a binary type and cannot be used as a file part.",
                        ClassUtils.getClassSimpleName(fileValue)));
            }
            paramInfos.add(new ParamInfo(name, toHttpFile(
                    fileValue,
                    context.parseExpression(binaryAnn.filename(), String.class),
                    context.parseExpression(binaryAnn.contentType(), String.class))));
        }

        return paramInfos;
    }

    /**
     * 校验表单项的条件表达式
     *
     * @param context   静态参数上下文
     * @param condition 条件表达式，为空时视为满足条件
     * @return 是否满足条件
     */
    private boolean isConditionMatched(StaticParamAnnContext context, String condition) {
        return !StringUtils.hasText(condition) || context.parseExpression(condition, boolean.class);
    }

    /**
     * 将资源类型参数转换为应用了文件名与Content-Type的 {@link HttpFile} 数组
     * <pre>
     *     资源参数支持的形态由{@link HttpExecutor#isResourceParam(Object)}判定，
     *     文件名与Content-Type为空时沿用各个文件自身的值。
     * </pre>
     *
     * @param resourceParam 资源类型参数
     * @param filename      自定义文件名，为空时沿用内容自带的文件名
     * @param contentType   自定义Content-Type，为空时沿用内容自带的Content-Type
     * @return 携带文件名与Content-Type的 HttpFile 数组
     */
    private HttpFile[] toHttpFiles(Object resourceParam, String filename, String contentType) {
        HttpFile[] httpFiles = HttpExecutor.toHttpFiles(resourceParam);
        HttpFile[] result = new HttpFile[httpFiles.length];
        for (int i = 0; i < httpFiles.length; i++) {
            HttpFile httpFile = httpFiles[i];
            String useFileName = StringUtils.hasText(filename) ? filename : httpFile.getFileName();
            result[i] = new HttpFile(httpFile, () -> useFileName, useContentType(contentType, httpFile.getContentType()), httpFile.getDescriptor());
        }
        return result;
    }

    /**
     * 将 {@link HttpExecutor#isBinaryParam(Object)} 判定通过的二进制参数转换为携带自定义文件名与
     * Content-Type的 {@link HttpFile} 实例。
     * <pre>
     *     资源系类型（File/Resource/MultipartFile/HttpFile）走各自的构造器，保留流式读取；
     *     InputStream按流包装；byte[]/Byte[]/ByteBuffer/Reader等其余二进制形态
     *     统一经{@link HttpExecutor#toByte(Object)}转成字节后包装。
     * </pre>
     *
     * @param param       二进制参数对象，支持 {@link HttpFile}、{@link java.io.File}、{@link Resource}、
     *                    {@link MultipartFile}、{@link InputStream}、{@link InputStreamSource}、
     *                    {@code byte[]}、{@code Byte[]}、{@code ByteBuffer}、{@code Reader}
     * @param filename    自定义文件名，将覆盖内容自带的文件名
     * @param contentType 自定义Content-Type，为空时沿用内容自带的Content-Type
     * @return 携带指定文件名的 HttpFile 实例
     */
    private HttpFile toHttpFile(Object param, String filename, String contentType) {
        if (param instanceof HttpFile) {
            HttpFile hf = (HttpFile) param;
            return new HttpFile(hf, () -> filename, useContentType(contentType, hf.getContentType()), hf.getDescriptor());
        }
        if (param instanceof java.io.File) {
            return new HttpFile((java.io.File) param, filename, contentType);
        }
        if (param instanceof Resource) {
            return new HttpFile((Resource) param, filename, contentType);
        }
        if (param instanceof MultipartFile) {
            MultipartFile mf = (MultipartFile) param;
            return new HttpFile(mf, filename, useContentType(contentType, mf.getContentType()));
        }
        if (param instanceof InputStream) {
            return new HttpFile((InputStream) param, filename, contentType);
        }
        if (param instanceof InputStreamSource) {
            return new HttpFile((InputStreamSource) param, () -> filename, contentType, "[InputStreamSource] " + filename);
        }
        // byte[]/Byte[]/ByteBuffer/Reader：统一转成字节后包装
        return new HttpFile(HttpExecutor.toByte(param), filename, contentType);
    }

    /**
     * 注解上显式指定的Content-Type优先，为空时沿用内容自带的Content-Type
     *
     * @param annotationContentType 注解上声明的Content-Type
     * @param originContentType     内容自带的Content-Type
     * @return 最终使用的Content-Type
     */
    private String useContentType(String annotationContentType, String originContentType) {
        return StringUtils.hasText(annotationContentType) ? annotationContentType : originContentType;
    }
}
