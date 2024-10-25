package com.luckyframework.aop;

import com.luckyframework.annotations.DisableProxy;
import com.luckyframework.aop.advice.AfterAdvice;
import com.luckyframework.aop.advice.AfterReturningAdvice;
import com.luckyframework.aop.advice.AfterThrowingAdvice;
import com.luckyframework.aop.advice.BeforeAdvice;
import com.luckyframework.aop.advice.MethodInterceptor;
import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.aop.aspectj.AspectJExpressionGlobalPointcutManagement;
import com.luckyframework.aop.aspectj.DefaultAdvisor;
import com.luckyframework.aop.aspectj.ExpressionGlobalPointcutManagement;
import com.luckyframework.aop.exception.AopParamsConfigurationException;
import com.luckyframework.aop.exception.PositionExpressionConfigException;
import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.order.OrderRelated;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@DisableProxy
public class AspectJAdvisorBatchProductionPlant implements ApplicationContextAware ,AdvisorBatchProductionPlant{

    private final static Class<?> JOIN_POINT_TYPE = JoinPoint.class;
    private ApplicationContext applicationContext;
    private final ExpressionGlobalPointcutManagement pointcutManagement;
    private List<Advisor> advisors;

    public AspectJAdvisorBatchProductionPlant() {
        this.pointcutManagement = new AspectJExpressionGlobalPointcutManagement();
    }

    public void registryAdvisor(Advisor advisor) {
        this.advisors.add(advisor);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Collection<Advisor> getAdvisors() {
        if(advisors == null){
            try {
                advisors = new ArrayList<>();
                registryAdvisor();
            }catch (IOException e){
                throw new LuckyIOException("An exception occurred while registering the aspectj advisor",e);
            }
        }
        return advisors;
    }

    private void registryAdvisor() throws IOException {
        pointcutManagementSetting();
        registryMethodAdvisor();
    }

    // 解析@Aspect组件中的@Pointcut方法中的表达式
    private void pointcutManagementSetting() {
        String[] aspectBeanNames = applicationContext.getBeanNamesForAnnotation(Aspect.class);
        for (String aspectBeanName : aspectBeanNames) {
            Class<?> aspectClass = applicationContext.getType(aspectBeanName);
            String aspectClassName = aspectClass.getName();
            List<Method> pointcutMethods = ClassUtils.getMethodByAnnotation(aspectClass, Pointcut.class);
            for (Method pointcutMethod : pointcutMethods) {
                Pointcut pointcut = pointcutMethod.getAnnotation( Pointcut.class);
                String prefix = aspectClassName+ ExpressionGlobalPointcutManagement.CONNECTOR+pointcutMethod.getName()+ ExpressionGlobalPointcutManagement.PARENTHESES;
                pointcutManagement.addExpressionPointcut(prefix,pointcut.value());
            }
        }
    }

    // 解析并注册@Aspect组件中的Advisor
    private void registryMethodAdvisor() throws IOException {
        String[] aspectBeanNames = applicationContext.getBeanNamesForAnnotation(Aspect.class);
        for (String aspectBeanName : aspectBeanNames) {
            Class<?> aspectClass = applicationContext.getType(aspectBeanName);
            Object aspectBean = applicationContext.getBean(aspectBeanName);
            int aspectBeanPriority = OrderRelated.getOrder(aspectBean);
            List<Method> allMethods = ClassUtils.getAllMethods(aspectClass);
            for (Method method : allMethods) {
                if(method.isAnnotationPresent(Around.class)){
                    registryAroundAdvisor(aspectBeanName,method,aspectBeanPriority);
                    continue;
                }
                if(method.isAnnotationPresent(After.class)){
                    registryAfterAdvisor(aspectBeanName,method,aspectBeanPriority);
                    continue;
                }
                if(method.isAnnotationPresent(AfterThrowing.class)){
                    registryAfterThrowingAdvisor(aspectBeanName,method,aspectBeanPriority);
                    continue;
                }
                if(method.isAnnotationPresent(AfterReturning.class)){
                    registryAfterReturningAdvisor(aspectBeanName,method,aspectBeanPriority);
                    continue;
                }
                if(method.isAnnotationPresent(Before.class)){
                    registryBeforeAdvisor(aspectBeanName,method,aspectBeanPriority);
                }
            }
        }
    }

    // 注册前置增强 @Before
    private void registryBeforeAdvisor(String aspectBeanName, Method beforeMethod,int aspectClassPriority){
        Parameter[] parameters = beforeMethod.getParameters();
        if(parameters.length == 1 && !parameters[0].getType().equals(JOIN_POINT_TYPE)){
            throw new AopParamsConfigurationException("The after-advice parameter configuration is incorrect. The method can have no parameters or one parameter, and the type of the parameter must be 'org.aspectj.lang.JoinPoint'. Error location:'"+beforeMethod+"'");
        }
        final Object[] aspectMethodRunningArgs = new Object[parameters.length];
        Before before = beforeMethod.getAnnotation(Before.class);
        Object aspectBean = applicationContext.getBean(aspectBeanName);
        String standbyExpression = before.value();
        String prefix = aspectBean.getClass().getName()+ ExpressionGlobalPointcutManagement.CONNECTOR+standbyExpression;
        com.luckyframework.aop.pointcut.Pointcut  pointcut = pointcutManagement.getPointcutByExpression(prefix,standbyExpression);
        BuiltInSortingBeforeAdvice beforeAdvice = new BuiltInSortingBeforeAdvice() {
            @Override
            public void before(JoinPoint joinPoint) {
                if(!ContainerUtils.isEmptyArray(aspectMethodRunningArgs)){
                    aspectMethodRunningArgs[0] = joinPoint;
                }
                MethodUtils.invoke(aspectBean,beforeMethod,aspectMethodRunningArgs);
            }

            @Override
            public int getOrder() {
                if(OrderRelated.isOrderMethod(beforeMethod)){
                    return OrderRelated.getOrder(beforeMethod);
                }
                return aspectClassPriority;
            }
        };

        registryAdvisor(new DefaultAdvisor(beforeAdvice,pointcut));
    }

    // 注册后置增强 @After
    private void registryAfterAdvisor(String aspectBeanName, Method afterMethod,int aspectClassPriority){
        Parameter[] parameters = afterMethod.getParameters();
        if(parameters.length ==1 && !parameters[0].getType().equals(JOIN_POINT_TYPE)){
            throw new AopParamsConfigurationException("The after-advice parameter configuration is incorrect. The method can have no parameters or one parameter, and the type of the parameter must be 'org.aspectj.lang.JoinPoint'. Error location:'"+afterMethod+"'");
        }
        final Object[] aspectMethodRunningArgs = new Object[parameters.length];
        After after = afterMethod.getAnnotation(After.class);
        Object aspectBean = applicationContext.getBean(aspectBeanName);
        String standbyExpression = after.value();
        String prefix = aspectBean.getClass().getName()+ ExpressionGlobalPointcutManagement.CONNECTOR+standbyExpression;
        com.luckyframework.aop.pointcut.Pointcut  pointcut = pointcutManagement.getPointcutByExpression(prefix,standbyExpression);
        BuiltInSortingAfterAdvice afterAdvice = new BuiltInSortingAfterAdvice(){
            @Override
            public int getOrder() {
                if(OrderRelated.isOrderMethod(afterMethod)){
                    return OrderRelated.getOrder(afterMethod);
                }
                return aspectClassPriority;
            }

            @Override
            public void after(JoinPoint joinPoint) {
                if(!ContainerUtils.isEmptyArray(aspectMethodRunningArgs)){
                    aspectMethodRunningArgs[0] = joinPoint;
                }
                MethodUtils.invoke(aspectBean,afterMethod,aspectMethodRunningArgs);
            }
        };
        registryAdvisor(new DefaultAdvisor(afterAdvice,pointcut));
    }

    // 注册后置增强 @AfterReturning
    private void registryAfterReturningAdvisor(String aspectBeanName, Method afterReturningMethod,int aspectClassPriority) throws IOException {
        AfterReturning afterReturning = afterReturningMethod.getAnnotation(AfterReturning.class);
        String resultName = afterReturning.returning();
        returningAndThrowingParamCheck("AfterReturning",resultName,afterReturningMethod);
        List<String> paramNames = ASMUtil.getClassOrInterfaceMethodParamNames(afterReturningMethod);
        Parameter[] parameters = afterReturningMethod.getParameters();
        final Object[] aspectMethodRunningArgs = new Object[parameters.length];
        Object aspectBean = applicationContext.getBean(aspectBeanName);
        String standbyExpression = getNotEmptyString(afterReturning.pointcut(),afterReturning.value());
        if(standbyExpression == null) throw new PositionExpressionConfigException("afterReturning",afterReturningMethod);
        String prefix = aspectBean.getClass().getName()+ ExpressionGlobalPointcutManagement.CONNECTOR+standbyExpression;
        com.luckyframework.aop.pointcut.Pointcut  pointcut = pointcutManagement.getPointcutByExpression(prefix,standbyExpression);
        BuiltInSortingAfterReturningAdvice afterReturningAdvice = new BuiltInSortingAfterReturningAdvice(){

            @Override
            public int getOrder() {
                if(OrderRelated.isOrderMethod(afterReturningMethod)){
                    return OrderRelated.getOrder(afterReturningMethod);
                }
                return aspectClassPriority;
            }

            @Override
            public void afterReturning(JoinPoint joinPoint, Object returning) {
                for (int i = 0,j =parameters.length ; i < j; i++) {
                    if(JOIN_POINT_TYPE.equals(parameters[i].getType())){
                        aspectMethodRunningArgs[i] = joinPoint;
                        continue;
                    }
                    if(resultName.equals(paramNames.get(i))){
                        aspectMethodRunningArgs[i] = returning;
                        continue;
                    }
                    aspectMethodRunningArgs[i] = null;
                }
                MethodUtils.invoke(aspectBean,afterReturningMethod,aspectMethodRunningArgs);
            }
        };
        registryAdvisor(new DefaultAdvisor(afterReturningAdvice,pointcut));
    }

    // 注册后置增强 @AfterThrowing
    private void registryAfterThrowingAdvisor(String aspectBeanName, Method afterThrowingMethod,int aspectClassPriority) throws IOException {
        AfterThrowing afterThrowing = afterThrowingMethod.getAnnotation(AfterThrowing.class);
        String resultName = afterThrowing.throwing();
        returningAndThrowingParamCheck("AfterThrowing",resultName,afterThrowingMethod);
        List<String> paramNames = ASMUtil.getClassOrInterfaceMethodParamNames(afterThrowingMethod);
        Parameter[] parameters = afterThrowingMethod.getParameters();
        final Object[] aspectMethodRunningArgs = new Object[parameters.length];
        Object aspectBean = applicationContext.getBean(aspectBeanName);
        String standbyExpression = getNotEmptyString(afterThrowing.pointcut(),afterThrowing.value());
        if(standbyExpression == null) throw new PositionExpressionConfigException("afterReturning",afterThrowingMethod);
        String prefix = aspectBean.getClass().getName()+ ExpressionGlobalPointcutManagement.CONNECTOR+standbyExpression;
        com.luckyframework.aop.pointcut.Pointcut pointcut = pointcutManagement.getPointcutByExpression(prefix,standbyExpression);
        BuiltInSortingAfterThrowingAdvice afterThrowingAdvice = new BuiltInSortingAfterThrowingAdvice(){

            @Override
            public int getOrder() {
                if(OrderRelated.isOrderMethod(afterThrowingMethod)){
                    return OrderRelated.getOrder(afterThrowingMethod);
                }
                return aspectClassPriority;
            }

            @Override
            public void afterThrowing(JoinPoint joinPoint, Throwable e) {
                for (int i = 0,j =parameters.length ; i < j; i++) {
                    if(JOIN_POINT_TYPE.equals(parameters[i].getType())){
                        aspectMethodRunningArgs[i] = joinPoint;
                        continue;
                    }
                    if(resultName.equals(paramNames.get(i))){
                        aspectMethodRunningArgs[i] = e;
                        continue;
                    }
                    aspectMethodRunningArgs[i] = null;
                }
                MethodUtils.invoke(aspectBean,afterThrowingMethod,aspectMethodRunningArgs);
            }
        };
        registryAdvisor(new DefaultAdvisor(afterThrowingAdvice,pointcut));
    }

    // 注册后置增强 @Around
    private void registryAroundAdvisor(String aspectBeanName, Method aroundMethod,int aspectClassPriority){
        Parameter[] parameters = aroundMethod.getParameters();
        if(parameters.length == 1 && !JOIN_POINT_TYPE.isAssignableFrom(parameters[0].getType())){
            throw new AopParamsConfigurationException("The around-advice parameter configuration is incorrect. The method can have no parameters or one parameter, and the type of the parameter must be 'org.aspectj.lang.JoinPoint'. Error location:'"+aroundMethod+"'");
        }
        if(aroundMethod.getReturnType() == void.class){
            throw new AopParamsConfigurationException("The around-advice return value was misconfigured. The method must have a return value. Error location:'"+aroundMethod+"'");
        }
        final Object[] aspectMethodRunningArgs = new Object[parameters.length];
        Around before = aroundMethod.getAnnotation(Around.class);
        Object aspectBean = applicationContext.getBean(aspectBeanName);
        String standbyExpression = before.value();
        String prefix = aspectBean.getClass().getName()+ ExpressionGlobalPointcutManagement.CONNECTOR+standbyExpression;
        com.luckyframework.aop.pointcut.Pointcut  pointcut = pointcutManagement.getPointcutByExpression(prefix,standbyExpression);
        BuiltInSortingMethodInterceptor aroundAdvice = new BuiltInSortingMethodInterceptor(){

            @Override
            public int getOrder() {
                if(OrderRelated.isOrderMethod(aroundMethod)){
                    return OrderRelated.getOrder(aroundMethod);
                }
                return aspectClassPriority;
            }

            @Override
            public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
                if(!ContainerUtils.isEmptyArray(aspectMethodRunningArgs)){
                    aspectMethodRunningArgs[0] = joinPoint;
                }
                return MethodUtils.invoke(aspectBean,aroundMethod,aspectMethodRunningArgs);
            }
        };
        registryAdvisor(new DefaultAdvisor(aroundAdvice,pointcut));
    }

    /*
        返回获选字符串中非空的字符
        1.str1非空时优先返回str1
        2.str2非空时返回str2
        3.str1、str2均为空时，返回null
    */
    private String getNotEmptyString(String str1,String str2){
        if(StringUtils.hasText(str1)){
            return str1;
        }
        if(StringUtils.hasText(str2)){
            return str2;
        }
        return null;
    }

    //todo 检验逻辑还未编写
    private void returningAndThrowingParamCheck(String type,String rt,Method method){
        Parameter[] parameters = method.getParameters();
        if("AfterReturning".equals(type)){

        }

        // type == AfterThrowing
        else{

        }
    }

    private boolean lengthCheck(String rt,int paramLength){
        if(StringUtils.hasText(rt)){
            return paramLength == 0 || paramLength == 1 || paramLength ==2;
        }
        return paramLength == 0 || paramLength == 1;
    }


    interface BuiltInSortingAfterAdvice extends AfterAdvice, Ordered {

    }

    interface BuiltInSortingAfterReturningAdvice extends AfterReturningAdvice,Ordered{

    }

    interface BuiltInSortingAfterThrowingAdvice extends AfterThrowingAdvice,Ordered{

    }

    interface BuiltInSortingBeforeAdvice extends BeforeAdvice,Ordered{

    }

    interface BuiltInSortingMethodInterceptor extends MethodInterceptor,Ordered{

    }
}
