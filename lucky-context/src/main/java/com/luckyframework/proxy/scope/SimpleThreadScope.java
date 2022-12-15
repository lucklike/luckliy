package com.luckyframework.proxy.scope;

import com.luckyframework.bean.factory.ObjectFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SimpleThreadScope implements Scope {
    private static final Log logger = LogFactory.getLog(SimpleThreadScope.class);

    private final ThreadLocal<Map<String, Object>> threadScope =
            new NamedThreadLocal<Map<String, Object>>("SimpleThreadScope") {
                @Override
                protected Map<String, Object> initialValue() {
                    return new HashMap<>();
                }
            };


    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Map<String, Object> scope = this.threadScope.get();
        // NOTE: Do NOT modify the following to use Map::computeIfAbsent. For details,
        // see https://github.com/spring-projects/spring-framework/issues/25801.
        Object scopedObject = scope.get(name);
        if (scopedObject == null) {
            scopedObject = objectFactory.getObject();
            scope.put(name, scopedObject);
        }
        return scopedObject;
    }

    @Override
    @Nullable
    public Object remove(String name) {
        Map<String, Object> scope = this.threadScope.get();
        return scope.remove(name);
    }
}
