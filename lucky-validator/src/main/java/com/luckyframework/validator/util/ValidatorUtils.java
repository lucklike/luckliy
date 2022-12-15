package com.luckyframework.validator.util;


import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;

import javax.validation.*;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/16 14:12
 */
public class ValidatorUtils {

    private static final Validator validator;
    private static final Validator failFastValidator;

    private static final ExecutableValidator executableValidator;
    private static final ExecutableValidator failFastExecutableValidator;

    @Nullable
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    static {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        HibernateValidatorConfiguration configure = Validation.byProvider(HibernateValidator.class).configure();
        configureParameterNameProvider(parameterNameDiscoverer, configure);
        failFastValidator = configure.failFast(true).buildValidatorFactory().getValidator();
        executableValidator = validator.forExecutables();
        failFastExecutableValidator = failFastValidator.forExecutables();
    }


    public static Validator getValidator() {
        return validator;
    }

    public static Validator getFailFastValidator() {
        return failFastValidator;
    }

    public static ExecutableValidator getExecutableValidator() {
        return executableValidator;
    }

    public static ExecutableValidator getFailFastExecutableValidator() {
        return failFastExecutableValidator;
    }

    public static void failFastValidate(Object object, Class<?>...groupClasses){
        Set<ConstraintViolation<Object>> validate = ValidatorUtils.getFailFastValidator().validate(object, groupClasses);
        if(!validate.isEmpty()){
            throw new ConstraintViolationException(validate);
        }
    }

    public static void failFastMethodParameterValidate(Object object, Method method, Object[] args, Class<?>...groupClasses){
        Set<ConstraintViolation<Object>> validate = ValidatorUtils.getExecutableValidator().validateParameters(object, method, args, groupClasses);
        if(!validate.isEmpty()){
            throw new ConstraintViolationException(validate);
        }
    }

    public static void failFastMethodReturnValue(Object object, Method method, Object[] args, Class<?>...groupClasses){
        Set<ConstraintViolation<Object>> validate = ValidatorUtils.getExecutableValidator().validateReturnValue(object, method, args, groupClasses);
        if(!validate.isEmpty()){
            throw new ConstraintViolationException(validate);
        }
    }

    private static void configureParameterNameProvider(ParameterNameDiscoverer discoverer, Configuration<?> configuration) {
        final ParameterNameProvider defaultProvider = configuration.getDefaultParameterNameProvider();
        configuration.parameterNameProvider(new ParameterNameProvider() {
            @Override
            public List<String> getParameterNames(Constructor<?> constructor) {
                String[] paramNames = discoverer.getParameterNames(constructor);
                return (paramNames != null ? Arrays.asList(paramNames) :
                        defaultProvider.getParameterNames(constructor));
            }
            @Override
            public List<String> getParameterNames(Method method) {
                String[] paramNames = discoverer.getParameterNames(method);
                return (paramNames != null ? Arrays.asList(paramNames) :
                        defaultProvider.getParameterNames(method));
            }
        });
    }
}
