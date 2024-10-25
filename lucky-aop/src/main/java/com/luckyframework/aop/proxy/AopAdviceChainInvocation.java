package com.luckyframework.aop.proxy;

import com.luckyframework.aop.advice.Advice;
import com.luckyframework.aop.advice.AfterAdvice;
import com.luckyframework.aop.advice.AfterReturningAdvice;
import com.luckyframework.aop.advice.AfterThrowingAdvice;
import com.luckyframework.aop.advice.BeforeAdvice;
import com.luckyframework.aop.advice.MethodInterceptor;
import com.luckyframework.aop.aspectj.MethodInterceptorJoinPoint;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 14:06
 */
public class AopAdviceChainInvocation {

    private final MethodInterceptorJoinPoint joinPoint;
    private final String beanName;
    private Object target;
    private final Object proxy;
    private Object[] args;
    private final Method method;
    private final MethodProxy methodProxy;
    private final List<Advice> advices;
    private int index = 0;

    public void setTarget(Object target) {
        this.target = target;
    }

    public AopAdviceChainInvocation(Object proxy, String beanName, Object target, Method method, Object[] args, List<Advice> advices) {
        this(proxy,beanName,target,method,null,args,advices);
    }

    public AopAdviceChainInvocation(Object proxy,String beanName,Object target, Method method,MethodProxy methodProxy, Object[] args, List<Advice> advices) {
        this.beanName = beanName;
        this.proxy = proxy;
        this.target = target;
        this.method = method;
        this.advices = advices;
        this.args = args;
        this.methodProxy = methodProxy;
        this.joinPoint = new MethodInterceptorJoinPoint(this);
    }

    public Object getProxy() {
        return proxy;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public List<Advice> getAdvices() {
        return advices;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getBeanName() {
        return beanName;
    }

    public Object invoke() throws Throwable{
        if(index<this.advices.size()){
            Object advice = advices.get(index++);
            //前置增加
            if(advice instanceof BeforeAdvice){
                ((BeforeAdvice)advice).before(joinPoint);
            }
            //正常执行的后置增强
            else if(advice instanceof AfterReturningAdvice){
                Object result = this.invoke();
                ((AfterReturningAdvice)advice).afterReturning(joinPoint,result);
                return result;
            }
            //执行异常的后置增强
            else if(advice instanceof AfterThrowingAdvice){
                try {
                    return this.invoke();
                }catch (Throwable e){
                    ((AfterThrowingAdvice)advice).afterThrowing(joinPoint,e);
                }
            }

            //后置增强
            else if(advice instanceof AfterAdvice){
                try {
                    return this.invoke();
                }finally {
                    ((AfterAdvice)advice).after(joinPoint);
                }
            }
            //环绕增强
            else if(advice instanceof MethodInterceptor){
                return ((MethodInterceptor)advice).invoke(joinPoint);
            }
            return this.invoke();
        }else{
            if(methodProxy == null){
                return MethodUtils.invoke(target,method,args);
            }
            return methodProxy.invokeSuper(proxy,args);
        }
    }
}
