package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.common.ObjectUtils;
import com.luckyframework.httpclient.core.meta.BodyObjectFactory;
import com.luckyframework.httpclient.core.meta.DynamicElementAddBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.FlatBeanBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import static com.luckyframework.httpclient.core.meta.ContentType.APPLICATION_JSON;

/**
 * 动态添加JSON元素的参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:40
 */
public class DynamicAddJsonElementParameterSetter implements ParameterSetter {


    /**
     * 将参数设置到工厂类中
     *
     * @param request   请求体
     * @param paramInfo 参数信息
     */
    @Override
    public void set(Request request, ParamInfo paramInfo) {
        if (paramInfo.getValue() != null) {
            DynamicElementAddBodyObjectFactory bodyObjectFactory = getOrInitBodyObjectFactory(request, paramInfo);
            bodyObjectFactory.addElement(String.valueOf(paramInfo.getName()), paramInfo.getValue());
        }
    }


    /**
     * 获取或者初始化一个支持动态添加元素的{@link BodyObjectFactory}
     *
     * @param request   请求实例
     * @param paramInfo 参数信息
     * @return 支持动态添加元素的 {@link BodyObjectFactory}
     */
    private synchronized DynamicElementAddBodyObjectFactory getOrInitBodyObjectFactory(Request request, ParamInfo paramInfo) {
        BodyObjectFactory bodyFactory = request.getBodyFactory();
        if (!(bodyFactory instanceof DynamicElementAddBodyObjectFactory)) {
            if (ObjectUtils.firstIsArrayKey(String.valueOf(paramInfo.getName()))) {
                bodyFactory = FlatBeanBodyObjectFactory.jsonList();
            } else {
                bodyFactory = FlatBeanBodyObjectFactory.jsonMap();
            }
            request.setContentType(APPLICATION_JSON);
            request.setBodyFactory(bodyFactory);
        }
        return (DynamicElementAddBodyObjectFactory) bodyFactory;
    }
}
