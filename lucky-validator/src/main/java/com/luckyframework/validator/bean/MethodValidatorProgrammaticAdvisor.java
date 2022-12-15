package com.luckyframework.validator.bean;

import com.luckyframework.aop.advisor.ProgrammaticAdvisor;
import com.luckyframework.bean.aware.BeanFactoryAware;
import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.exception.BeanCreationException;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.validator.annotations.ValidationAnnotationUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/21 10:47
 */
public class MethodValidatorProgrammaticAdvisor implements ProgrammaticAdvisor, BeanFactoryAware {

    private Validator validator;

    @Override
    public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method methodToValidate = signature.getMethod();

        Object target = joinPoint.getTarget();
        Class<?> targetClass = target.getClass();
        Object[] args = joinPoint.getArgs();


        Class<?>[] groups = ValidationAnnotationUtils.getGroupClasses(targetClass, methodToValidate);

        // Standard Bean Validation 1.1 API
        ExecutableValidator execVal = this.validator.forExecutables();
        Set<ConstraintViolation<Object>> result;

        Assert.state(target != null, "Target must not be null");

        try {
            result = execVal.validateParameters(target, methodToValidate, args, groups);
        }
        catch (IllegalArgumentException ex) {
            // Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
            // Let's try to find the bridged method on the implementation class...
            methodToValidate = BridgeMethodResolver.findBridgedMethod(
                    ClassUtils.getMostSpecificMethod(signature.getMethod(), target.getClass()));
            result = execVal.validateParameters(target, methodToValidate, args, groups);
        }
        if (!result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }

        Object returnValue = joinPoint.proceed();

        result = execVal.validateReturnValue(target, methodToValidate, returnValue, groups);
        if (!result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }

        return returnValue;
    }

    @Override
    public boolean matchClass(String currentBeanName, Class<?> targetClass) {
        return ValidationAnnotationUtils.isValidated(targetClass);
    }

    @Override
    public boolean matchMethod(Class<?> targetClass, Method method, Object... args) {
        return !MethodUtils.isObjectMethod(method) && (ValidationAnnotationUtils.isValidated(targetClass) || ValidationAnnotationUtils.isValidated(method));
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        ValidatorFactory validatorFactory = beanFactory.getBean(ValidatorFactory.class);
        if(validatorFactory != null){
            this.validator = validatorFactory.getValidator();
        }
        else{
            this.validator = beanFactory.getBean(Validator.class);
        }
        if(this.validator == null){
            throw new BeanCreationException("Neither 'Validator' nor 'ValidatorFactory' was found, unable to create a 'MethodValidatorProgrammaticAdvisor' instance");
        }
    }
}
