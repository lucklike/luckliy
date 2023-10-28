package com.luckyframework.spel;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 属性不存在则返回null的属性转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:45
 */
public class NotExistReturnNullMapAccessor implements CompilablePropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] {Map.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return target instanceof Map;
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        Assert.state(target instanceof Map, "Target must be of type Map");
        return new TypedValue(((Map<?, ?>) target).get(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
            throws AccessException {

        Assert.state(target instanceof Map, "Target must be a Map");
        Map<Object, Object> map = (Map<Object, Object>) target;
        map.put(name, newValue);
    }

    @Override
    public boolean isCompilable() {
        return true;
    }

    @Override
    public Class<?> getPropertyType() {
        return Object.class;
    }

    @Override
    public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
        String descriptor = cf.lastDescriptor();
        if (descriptor == null || !descriptor.equals("Ljava/util/Map")) {
            if (descriptor == null) {
                cf.loadTarget(mv);
            }
            CodeFlow.insertCheckCast(mv, "Ljava/util/Map");
        }
        mv.visitLdcInsn(propertyName);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get","(Ljava/lang/Object;)Ljava/lang/Object;",true);
    }
}
