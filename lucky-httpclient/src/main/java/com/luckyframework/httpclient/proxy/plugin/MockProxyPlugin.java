package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerateUtil;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.lang.NonNull;

/**
 * Mock插件实现类
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public class MockProxyPlugin implements ProxyPlugin {

    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {

        ExecuteMeta meta = decorator.getMeta();
        MethodMetaContext metaContext = meta.getMetaContext();
        ClassContext classContext = metaContext.lookupContext(ClassContext.class);


        MockPlugin mockPluginAnn = classContext.getMergedAnnotation(MockPlugin.class);

        // 开关关闭时执行原方法
        String enable = mockPluginAnn.enable();
        if (StringUtils.hasText(enable) && !metaContext.parseExpression(enable, boolean.class)) {
            return decorator.proceed();
        }

        // 获取Mock实现类，执行Mock逻辑
        Object implObject = getImplObject(metaContext, mockPluginAnn);
        return MethodUtils.invoke(implObject, meta.getMethod(), meta.getArgs());
    }


    @NonNull
    private Object getImplObject(MethodMetaContext metaContext, MockPlugin mockPluginAnn) {

        // 获取当前API类的类型
        Class<?> currApiClass = metaContext.lookupContext(ClassContext.class).getCurrentAnnotatedElement();

        // 优先使用生成器来生成实现类对象
        ObjectGenerate generate = mockPluginAnn.generate();
        if (ObjectGenerateUtil.isEffectiveObjectGenerate(generate, Void.class)) {
            Object implObject = metaContext.generateObject(generate);
            checkImplObject(implObject, currApiClass);
            return implObject;
        }

        // 其次使用Class来生成实现类对象
        Class<?> implClass = mockPluginAnn.implClass();
        if (implClass != Void.class) {
            checkImplClass(implClass, currApiClass);
            Object implObject = metaContext.generateObject(implClass, Scope.SINGLETON);
            checkImplObject(implObject, currApiClass);
            return implObject;
        }

        String implExp = mockPluginAnn.implExp();
        if (StringUtils.hasText(implExp)) {
            Object implObject = metaContext.parseExpression(implExp);
            checkImplObject(implObject, currApiClass);
            return implObject;
        }

        throw new IllegalArgumentException("Mock plugin executed exception：No applicable Mock configuration was found.");
    }

    /**
     * 检验实现类Class是否是基本类的子类
     *
     * @param implClass 实现类Class
     * @param baseClass 基本类Class
     */
    private void checkImplClass(Class<?> implClass, Class<?> baseClass) {
        if (!baseClass.isAssignableFrom(implClass)) {
            throw new ClassCastException(String.format("Mock plugin executed exception：%s is not assignable to %s", implClass, baseClass));
        }
    }

    /**
     * 检验实现类对象是否为基本类的实例
     *
     * @param implObject 实现类对象
     * @param baseClass  基本类Class
     */
    private void checkImplObject(Object implObject, Class<?> baseClass) {
        if (implObject == null) {
            throw new IllegalArgumentException("Mock plugin executed exception：implObject is null");
        }
        checkImplClass(implObject.getClass(), baseClass);
    }


}
