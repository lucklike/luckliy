package com.luckyframework.proxy.scope;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.event.ApplicationEvent;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.Set;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/29 16:38
 */
public class EnvRefreshedEvent extends ApplicationEvent {

    public static final EnvRefreshedEvent RELOAD_ALL = new EnvRefreshedEvent();


    public EnvRefreshedEvent(String...needRefreshBeanNames) {
        super(needRefreshBeanNames);
    }

    public Set<String> getNeedReloadBeanNames() {
        if(isReloadAll()){
            return null;
        }
        return ConversionUtils.conversion(getSource(), new SerializationTypeToken<Set<String>>() {
        });
    }

    public boolean isReloadAll(){
        Object source = getSource();
        if(source == null){
            return true;
        }
        String[] beanNames = (String[]) source;
        return ContainerUtils.isEmptyArray(beanNames);
    }
}
