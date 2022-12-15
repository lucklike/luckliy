package com.luckyframework.proxy;


import org.springframework.cglib.core.DefaultNamingPolicy;
import org.springframework.cglib.core.Predicate;

/**
 * CGLIB代理类的命名规则
 * @author fk7075
 * @version 1.0.0
 * @date 2020/11/28 上午2:17
 */
public class LuckyNamingPolicy extends DefaultNamingPolicy {


    //--------------------------------------------------
    //              $$ByLuckyCGLIB$$
    //--------------------------------------------------

    @Override
    protected String getTag() {
        return "CGLIB";
    }

    @Override
    public String getClassName(String prefix, String source, Object key, Predicate names) {
        return super.getClassName(prefix, "ByLucky", key, names);
    }
}
