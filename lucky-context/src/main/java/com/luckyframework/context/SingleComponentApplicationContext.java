package com.luckyframework.context;

import org.springframework.core.type.AnnotationMetadata;

/**
 * 单个注解组件的应用程序上下文
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/14 下午5:22
 */
public class SingleComponentApplicationContext extends AbstractBasedAnnotationApplicationContext {

    public SingleComponentApplicationContext(Class<?> configurationClass){
        super(AnnotationMetadata.introspect(configurationClass));
        init();
    }

    public static SingleComponentApplicationContext create(Class<?> configurationClass){
        SingleComponentApplicationContext content = new SingleComponentApplicationContext(configurationClass);
        content.refresh();
        return content;
    }

}
