package com.luckyframework.httpclient.generalapi.plugin;

import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerateUtil;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * Mock插件实现类
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public class MockProxyPlugin implements ProxyPlugin {

    private static final Logger logger = LoggerFactory.getLogger(MockProxyPlugin.class);

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
        printDebugLog(implObject, classContext.getCurrentAnnotatedElement());
        return executeMockImplMethod(implObject, meta);
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

        // 使用SpEL表达式来获取一个Mock实现类
        String implExp = mockPluginAnn.implExp();
        if (StringUtils.hasText(implExp)) {
            Object implObject = metaContext.parseExpression(implExp);
            checkImplObject(implObject, currApiClass);
            return implObject;
        }

        throw new MockProxyPluginException("Mock plugin executed exception：No applicable Mock configuration was found.");
    }

    /**
     * 检验实现类Class是否是基本类的子类
     *
     * @param implClass 实现类Class
     * @param baseClass 基本类Class
     */
    private void checkImplClass(Class<?> implClass, Class<?> baseClass) {
        if (!baseClass.isAssignableFrom(implClass)) {
            throw new MockProxyPluginException(String.format("Mock plugin executed exception：%s is not assignable to %s", implClass, baseClass));
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
            throw new MockProxyPluginException("Mock plugin executed exception：implObject is null");
        }
        checkImplClass(implObject.getClass(), baseClass);
    }

    /**
     * 执行Mock实现类对象的方法
     *
     * @param implObject ock实现类对象
     * @param meta       要执行的方法元信息
     * @return 执行结果
     */
    private Object executeMockImplMethod(Object implObject, ExecuteMeta meta) throws Throwable {
        try {
            return MethodUtils.invoke(implObject, meta.getMethod(), meta.getArgs());
        } catch (LuckyInvocationTargetException e) {
            throw e.getCause();
        } catch (Exception e) {
            throw new MockProxyPluginException(e, "Mock plugin executed exception: ['{}']", FontUtil.getBlueUnderline(MethodUtils.getLocation(meta.getMethod())));
        }
    }

    private void printDebugLog(Object implObject, Class<?> implClass) {
        if (logger.isDebugEnabled()) {
            StringBuilder debug = new StringBuilder();
            debug.append("\n\t").append(FontUtil.getBackMulberryStr(" MockPlugin "));
            debug.append("\n\t").append("🐵").append(FontUtil.getWhiteUnderline(implClass.getName()));
            debug.append("\n\t").append("🙈").append(FontUtil.getWhiteUnderline(ClassUtils.getClassName(implObject)));
            logger.debug(debug.toString());
        }
    }

}
