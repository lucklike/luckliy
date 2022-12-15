package com.luckyframework.proxy.scope;

import com.luckyframework.annotations.Component;
import com.luckyframework.annotations.DisableProxy;
import com.luckyframework.bean.factory.ObjectFactory;
import com.luckyframework.context.event.ApplicationListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.proxy.scope.RefreshScope.REFRESH_SCOPE_BEAN_NAME;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/29 16:22
 */
@Component(REFRESH_SCOPE_BEAN_NAME)
@DisableProxy
public class RefreshScope implements Scope, ApplicationListener<EnvRefreshedEvent> {

    public static final String REFRESH_SCOPE_BEAN_NAME = "@lucky-refresh-scope";

    private final Map<String, Object> refreshCacheMap = new ConcurrentHashMap<>(32);


    @Override
    public synchronized Object get(String beanName, ObjectFactory<?> objectFactory) {
        Object bean = refreshCacheMap.get(beanName);
        if(bean == null) {
            bean = objectFactory.getObject();
            refreshCacheMap.put(beanName, bean);
        }
        return bean;
    }

    @Override
    public synchronized Object remove(String beanName) {
        return refreshCacheMap.remove(beanName);
    }

    @Override
    public synchronized void onApplicationEvent(EnvRefreshedEvent event) {
        if(event.isReloadAll()){
            refreshCacheMap.clear();
        }
        else{
            event.getNeedReloadBeanNames().forEach(this::remove);
        }
    }
}
