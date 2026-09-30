package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import java.lang.reflect.WildcardType;

import static org.springframework.core.ResolvableType.NONE;

/**
 * 单一泛型的包装类型解析器
 * <pre>
 *    1.存在泛型时返回具体的泛型类型
 *    2.不存在泛型、泛型无法解析或者泛型为通配符时回退为Object类型（通配符会优先尝试解析为其上界）
 * </pre>
 */
public abstract class SingleGenericPackTypeParser implements PackTypeParser {

    /**
     * Object类型对应的ResolvableType
     */
    private static final ResolvableType OBJECT_TYPE = ResolvableType.forClass(Object.class);


    @Override
    public ResolvableType getRealType(MethodContext mc, ResolvableType packType) {
        if (!packType.hasGenerics()) {
            return OBJECT_TYPE;
        }

        ResolvableType genericType = packType.getGeneric(0);
        if (genericType == NONE) {
            return OBJECT_TYPE;
        }

        // 通配符类型（如 ?、? extends X、? super X）解析为其上界，解析失败时回退为Object类型
        if (genericType.getType() instanceof WildcardType) {
            Class<?> boundType = genericType.resolve();
            return boundType == null ? OBJECT_TYPE : ResolvableType.forClass(boundType);
        }

        // 无法解析的类型（如未绑定的类型变量）回退为Object类型
        return genericType.resolve() == null ? OBJECT_TYPE : genericType;
    }

}
