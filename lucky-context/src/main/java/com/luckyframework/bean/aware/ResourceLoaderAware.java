package com.luckyframework.bean.aware;


import org.springframework.core.io.ResourceLoader;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/29 0029 10:01
 */
public interface ResourceLoaderAware {

    void setResourceLoader(ResourceLoader resourceLoader);

}
