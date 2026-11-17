package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.LazyValueConvertAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;

import java.util.List;
import java.util.Map;

public class ValueSpacePropertyAccessor extends LazyValueConvertAccessor {

    private final List<String> namespaces;

    /**
     * 变量定位结果的快照
     * <p>
     * Spring总是先调用{@link #canRead(EvaluationContext, Object, String)}再调用
     * {@link #readValue(EvaluationContext, Object, String)}，这里保存canRead阶段的定位结果，
     * 让read阶段不必再次遍历命名空间列表
     */
    private volatile Resolution resolution;

    public ValueSpacePropertyAccessor(List<String> namespaces) {
        this.namespaces = namespaces;
    }


    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{Map.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        Map<?, ?> map = (Map<?, ?>) target;
        for (String namespace : namespaces) {
            Object namespaceValue = map.get(namespace);
            if (namespaceValue instanceof Map) {
                Map<?, ?> namespaceMap = (Map<?, ?>) namespaceValue;
                if (namespaceMap.containsKey(name)) {
                    this.resolution = new Resolution(map, name, namespaceMap);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Object readValue(EvaluationContext context, Object target, String name) throws AccessException {
        Resolution current = this.resolution;
        if (current != null && current.matches(target, name)) {
            return current.namespaceMap.get(name);
        }

        // 兜底逻辑：canRead未被调用或者快照已失效时重新定位
        Map<?, ?> map = (Map<?, ?>) target;
        for (String namespace : namespaces) {
            Object namespaceValue = map.get(namespace);
            if (namespaceValue instanceof Map) {
                Map<?, ?> namespaceMap = (Map<?, ?>) namespaceValue;
                if (namespaceMap.containsKey(name)) {
                    return namespaceMap.get(name);
                }
            }
        }
        return null;
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an ValueSpacePropertyAccessor");
    }

    /**
     * 变量定位结果快照，为不可变对象，保证多线程下不会出现target与name错配
     */
    private static final class Resolution {

        private final Map<?, ?> target;

        private final String name;

        private final Map<?, ?> namespaceMap;

        private Resolution(Map<?, ?> target, String name, Map<?, ?> namespaceMap) {
            this.target = target;
            this.name = name;
            this.namespaceMap = namespaceMap;
        }

        private boolean matches(Object target, String name) {
            return this.target == target && this.name.equals(name);
        }
    }
}
