package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import static org.springframework.core.ResolvableType.NONE;

/**
 * 单一泛型的包装类型解析器
 * <pre>
 *    如果存在泛型，则返回具体的泛型类型，不存在泛型或者泛型类型为null时返回Object类型
 * </pre>
 */
public abstract class SingleGenericPackTypeParser implements PackTypeParser {

    /**
     * Object类型对应的ResolvableType
     */
    private static final ResolvableType OBJECT_TYPE = ResolvableType.forClass(Object.class);


    @Override
    public ResolvableType getRealType(MethodContext mc, ResolvableType packType) {
        if (packType.hasGenerics()) {
            ResolvableType genericType = packType.getGeneric(0);
            return genericType == NONE ? OBJECT_TYPE : genericType;
        }
        return OBJECT_TYPE;
    }

}
