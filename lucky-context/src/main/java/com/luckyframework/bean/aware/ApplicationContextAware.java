package com.luckyframework.bean.aware;


import com.luckyframework.context.ApplicationContext;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 11:57
 */
public interface ApplicationContextAware extends Aware {

    void setApplicationContext(ApplicationContext applicationContext);

}
