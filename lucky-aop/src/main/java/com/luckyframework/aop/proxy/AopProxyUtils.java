package com.luckyframework.aop.proxy;

import com.luckyframework.aop.advice.Advice;
import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.aop.pointcut.Pointcut;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.order.OrderRelated;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aop代理工具类
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 14:17
 */
public class AopProxyUtils {

    /**
     * 以不支持嵌套代理的方式执行方法
     * @param proxy         代理对象
     * @param beanName      真实对象的Bean的名称
     * @param target        真实对象
     * @param method        当前要执行的方法
     * @param args          当前要执行的方法的参数列表
     * @param matchAdvisor  匹配当前方法的切面结合
     * @return              代理方法执行后返回的结果
     * @throws Throwable
     */
    public synchronized static Object applyAdvices(Object proxy,String beanName, Object target, Method method,Object[] args,
                                      List<Advisor> matchAdvisor) throws Throwable{
        List<Advice> advices = getShouldApplyAdvices(target.getClass(), method, args, matchAdvisor);
        if(ContainerUtils.isEmptyCollection(advices)){
            return MethodUtils.invoke(target, method, args);
        }else{
            AopAdviceChainInvocation chain = new AopAdviceChainInvocation(proxy, beanName, target, method, args, advices);
            return chain.invoke();
        }
    }


    /**
     * 以支持嵌套代理的方式执行代理方法
     * @param proxy         代理对象
     * @param beanName      真实对象的Bean的名称
     * @param target        真实对象
     * @param method        当前要执行的方法
     * @param methodProxy   当前要执行的方法代理[CGLIB]
     * @param args          当前要执行的方法的参数列表
     * @param matchAdvisor  匹配当前方法的切面结合
     * @return              代理方法执行后返回的结果
     * @throws Throwable
     */
    public synchronized static Object applySupportNestingAdvices(Object proxy,String beanName, Object target, Method method,
                                                    MethodProxy methodProxy, Object[] args, List<Advisor> matchAdvisor) throws Throwable {
        List<Advice> advices = getShouldApplyAdvices(target.getClass(),method,args,matchAdvisor);
        if(ContainerUtils.isEmptyCollection(advices)){
            return methodProxy.invokeSuper(proxy,args);
        }else{
            AopAdviceChainInvocation chain = new AopAdviceChainInvocation(proxy,beanName,target,method,methodProxy,args,advices);
            return chain.invoke();
        }
    }

    /**
     * 筛选并返回所有能匹配当前方法的切面集合
     * @param targetClass   真实对象的类型
     * @param method        当前执行的方法
     * @param args          当前执行的方法的参数列表
     * @param matchAdvisors 能匹配当前真实对象的所有切面的集合
     * @return
     */
    private static List<Advice> getShouldApplyAdvices(Class<?> targetClass, Method method,Object[] args,
                                                     List<Advisor> matchAdvisors){
        if (ContainerUtils.isEmptyCollection(matchAdvisors)) {
            return null;
        }
        List<Advice> advices = new ArrayList<>();
        matchAdvisors = matchAdvisors.stream()
                .sorted(Comparator.comparing((adv)-> OrderRelated.getOrder(adv.getAdvice())))
                .collect(Collectors.toList());
        for (Advisor advisor : matchAdvisors) {
            Pointcut pointcut = advisor.getPointcut();
            if(pointcut.matchMethod(targetClass,method,args)){
                advices.add(advisor.getAdvice());
            }
        }
        return advices;
    }

    /**
     * 属性复制，将真实对象的非Final属性赋值给代理对象
     * @param proxy     代理对象
     * @param target    真实对象
     */
    public static void fieldCopy(Object proxy,Object target){
        Field[] allFields = ClassUtils.getAllFields(target.getClass());
        for (Field field : allFields) {
            if(Modifier.isFinal(field.getModifiers()) || field.getName().startsWith("CGLIB$")){
                continue;
            }
            FieldUtils.setValue(proxy,field,FieldUtils.getValue(target,field));
        }
    }

    /**
     * 判断某个类的实例是否可以用构造函数的方式来创建
     * @param context 应用程序上下文
     * @param aClass  类型
     * @return true/false
     */
    public static boolean isCanCreatedByConstructor(ApplicationContext context,Class<?> aClass){
        try {
            ClassUtils.getConstructor(aClass,null);
            return true;
        }catch (Exception e){
            try {
                Constructor<?> constructor = com.luckyframework.definition.ClassUtils.findConstructor(aClass);
                Class<?>[] parameterTypes = com.luckyframework.definition.ClassUtils.findConstructorParameterTypes(constructor);
                for (Class<?> parameterType : parameterTypes) {
                    context.getBean(parameterType);
                }
                return true;
            }catch (Exception ex){
                return false;
            }
        }
    }
}
