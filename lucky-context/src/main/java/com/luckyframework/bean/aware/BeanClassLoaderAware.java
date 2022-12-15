package com.luckyframework.bean.aware;

import com.luckyframework.bean.factory.InitializingBean;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/23 10:09
 */
public interface BeanClassLoaderAware extends Aware{

    /**
     * Callback that supplies the bean {@link ClassLoader class loader} to
     * a bean instance.
     * <p>Invoked <i>after</i> the population of normal bean properties but
     * <i>before</i> an initialization callback such as
     * {@link InitializingBean InitializingBean's}
     * {@link InitializingBean#afterPropertiesSet()}
     * method or a custom init-method.
     * @param classLoader the owning class loader
     */
    void setBeanClassLoader(ClassLoader classLoader);
}
