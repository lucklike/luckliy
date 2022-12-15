package com.luckyframework.bean.factory;

/**
 * 销毁接口，{@link BeanFactory#close()}方法调用时执行下面的destroy方法
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 11:17
 */
public interface DisposableBean {

    void destroy() throws Exception;
}
