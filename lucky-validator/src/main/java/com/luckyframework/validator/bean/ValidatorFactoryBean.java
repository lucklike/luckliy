package com.luckyframework.validator.bean;

import com.luckyframework.annotations.Component;
import com.luckyframework.annotations.DisableProxy;
import com.luckyframework.bean.aware.MessageSourceAware;
import com.luckyframework.context.message.MessageSource;
import com.luckyframework.processor.FactoryBean;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;

import javax.validation.Configuration;
import javax.validation.ParameterNameProvider;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/30 17:08
 */
@Component("hibernateValidator")
@DisableProxy
public class ValidatorFactoryBean implements FactoryBean<Validator>, MessageSourceAware {

    private MessageSource messageSource;

    @Nullable
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public Validator getBean() {
        HibernateValidatorConfiguration configure = Validation.byProvider(HibernateValidator.class).configure();
        configureParameterNameProvider(parameterNameDiscoverer, configure);
        configure.messageInterpolator(new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource)));
        return configure.failFast(true).buildValidatorFactory().getValidator();
    }

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
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
