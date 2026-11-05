package com.luckyframework.httpclient.proxy.generator.support;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.generator.GeneratedResponseJavaBean;

import java.util.Map;

/**
 * 用于测试 {@link GeneratedResponseJavaBean#allowedGenerateEntityMethodReturnTypes()} 返回值类型白名单的代理接口
 * <pre>
 *     所有方法共用同一个输出目录和包名，测试直接断言目标文件是否存在即可
 * </pre>
 */
public interface GeneratedJavaCodeTestApi {

    /**
     * 返回值类型Map命中白名单（allowedGenerateEntityMethodReturnTypes = Map.class）
     */
    @GeneratedResponseJavaBean(
            allowedGenerateEntityMethodReturnTypes = Map.class,
            savePath = "target/generated-return-type-test",
            packageName = "generated.returntype",
            className = "MapResultResponse"
    )
    Map<String, Object> mapResult(Request request);

    /**
     * 返回值类型String未命中白名单（allowedGenerateEntityMethodReturnTypes = Map.class）
     */
    @GeneratedResponseJavaBean(
            allowedGenerateEntityMethodReturnTypes = Map.class,
            savePath = "target/generated-return-type-test",
            packageName = "generated.returntype",
            className = "StringResultResponse"
    )
    String stringResult(Request request);

    /**
     * 未配置返回值类型白名单，不做类型限制，应生成
     */
    @GeneratedResponseJavaBean(
            savePath = "target/generated-return-type-test",
            packageName = "generated.returntype",
            className = "MapResultWithoutTypesResponse"
    )
    Map<String, Object> mapResultWithoutTypes(Request request);

}
