package com.luckyframework.httpclient.generalapi.plugin;

import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * 参数校验插件，使用此插件需要额外导入如下两个依赖包
 * <pre>
 *     {@code
 *     // 1.实现jakarta.validation规范的依赖包 例如：
 *      <dependency>
 *          <groupId>org.hibernate.validator</groupId>
 *          <artifactId>hibernate-validator</artifactId>
 *          <version>6.2.5.Final</version>
 *      </dependency>
 *
 *
 *     // 2.实现El表达式的依赖包 例如：
 *      <dependency>
 *          <groupId>org.apache.tomcat.embed</groupId>
 *          <artifactId>tomcat-embed-el</artifactId>
 *          <version>9.0.83</version>
 *      </dependency>
 *     }
 * </pre>
 * @see Validated
 */
public class ValidationPlugin implements ProxyPlugin {

    private final Validator validator;


    /**
     * Create a new MethodValidationInterceptor using a default JSR-303 validator underneath.
     */
    public ValidationPlugin() {
        this(Validation.buildDefaultValidatorFactory());
    }

    /**
     * Create a new MethodValidationInterceptor using the given JSR-303 ValidatorFactory.
     *
     * @param validatorFactory the JSR-303 ValidatorFactory to use
     */
    public ValidationPlugin(ValidatorFactory validatorFactory) {
        this(validatorFactory.getValidator());
    }

    /**
     * Create a new MethodValidationInterceptor using the given JSR-303 Validator.
     *
     * @param validator the JSR-303 Validator to use
     */
    public ValidationPlugin(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {

        ExecuteMeta meta = decorator.getMeta();

        // Avoid Validator invocation on FactoryBean.getObjectType/isSingleton
        if (isFactoryBeanMetadataMethod(meta.getMethod())) {
            return decorator.proceed();
        }

        Class<?>[] groups = determineValidationGroups(meta);

        // Standard Bean Validation 1.1 API
        ExecutableValidator execVal = this.validator.forExecutables();
        Method methodToValidate = meta.getMethod();
        Set<ConstraintViolation<Object>> result;

        Object target = meta.getProxy();
        Assert.state(target != null, "Target must not be null");

        try {
            result = execVal.validateParameters(target, methodToValidate, meta.getArgs(), groups);
        } catch (IllegalArgumentException ex) {
            // Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
            // Let's try to find the bridged method on the implementation class...
            methodToValidate = BridgeMethodResolver.findBridgedMethod(
                    ClassUtils.getMostSpecificMethod(meta.getMethod(), target.getClass()));
            result = execVal.validateParameters(target, methodToValidate, meta.getArgs(), groups);
        }
        if (!result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }

        Object returnValue = decorator.proceed();

        result = execVal.validateReturnValue(target, methodToValidate, returnValue, groups);
        if (!result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }

        return returnValue;
    }

    private boolean isFactoryBeanMetadataMethod(Method method) {
        Class<?> clazz = method.getDeclaringClass();

        // Call from interface-based proxy handle, allowing for an efficient check?
        if (clazz.isInterface()) {
            return ((clazz == FactoryBean.class || clazz == SmartFactoryBean.class) &&
                    !method.getName().equals("getObject"));
        }

        // Call from CGLIB proxy handle, potentially implementing a FactoryBean method?
        Class<?> factoryBeanType = null;
        if (SmartFactoryBean.class.isAssignableFrom(clazz)) {
            factoryBeanType = SmartFactoryBean.class;
        } else if (FactoryBean.class.isAssignableFrom(clazz)) {
            factoryBeanType = FactoryBean.class;
        }
        return (factoryBeanType != null && !method.getName().equals("getObject") &&
                ClassUtils.hasMethod(factoryBeanType, method));
    }

    /**
     * Determine the validation groups to validate against for the given method invocation.
     * <p>Default are the validation groups as specified in the {@link Validated} annotation
     * on the containing target class of the method.
     *
     * @param executeMeta the current ExecuteMeta
     * @return the applicable validation groups as a Class array
     */
    protected Class<?>[] determineValidationGroups(ExecuteMeta executeMeta) {
        Validated validatedAnn = executeMeta.getMetaContext().getMergedAnnotationCheckParent(Validated.class);
        return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
    }

}
