package com.luckyframework.common;

import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/1/4 18:21
 */
public class RunnableActuator {

    private boolean isRunning;

    private RunnableActuator(boolean isRunning){
        this.isRunning = isRunning;
    }

    public static RunnableActuator runnableActuator(){
        return new RunnableActuator(true);
    }

    /**
     * 如果满足条件则执行给定的逻辑，否则不执行
     * @param b         是否满足条件
     * @param runnable  给定的逻辑
     */
    public static void trueIsRunning(boolean b, Runnable runnable){
        if(b){
            runnable.run();
        }
    }

    /**
     * 如果主流程执行出现异常则执行异常流程
     * @param mainRunnable              主流程
     * @param exceptionAfterRunnable    异常流程
     */
    public static void exceptionAfterRunning(Runnable mainRunnable, Runnable exceptionAfterRunnable){
        try {
            mainRunnable.run();
        }catch (Throwable e){
            exceptionAfterRunnable.run();;
        }
    }

    public RunnableActuator isTrue(boolean isRunning){
        if(this.isRunning){
            this.isRunning = isRunning;
        }
        return this;
    }

    public RunnableActuator isFalse(boolean notRunning){
        if(this.isRunning){
            this.isRunning = !notRunning;
        }
        return this;
    }

    public RunnableActuator isException(Runnable mainRunnable){
        if(this.isRunning){
            try {
                mainRunnable.run();
                this.isRunning = false;
            }catch (Throwable e){
                this.isRunning = true;
            }
        }
        return this;
    }

    public RunnableActuator isNotException(Runnable mainRunnable){
        if(this.isRunning){
            try {
                mainRunnable.run();
                this.isRunning = true;
            }catch (Throwable e){
                this.isRunning = false;
            }
        }
        return this;
    }


    public RunnableActuator isNotNull(Object obj){
        return isFalse(Objects.isNull(obj));
    }

    public RunnableActuator isNull(Object obj){
        return isTrue(Objects.isNull(obj));
    }

    public RunnableActuator isEmptyString(String str){
        return isFalse(StringUtils.hasLength(str));
    }

    public RunnableActuator isNotEmptyString(String str){
        return isTrue(StringUtils.hasLength(str));
    }

    public RunnableActuator isEmptyArray(Object[] array){
        return isTrue(ContainerUtils.isEmptyArray(array));
    }

    public RunnableActuator isNotEmptyArray(Object[] array){
        return isFalse(ContainerUtils.isEmptyArray(array));
    }

    public RunnableActuator isEmptyCollection(Collection<?> collection){
        return isTrue(ContainerUtils.isEmptyCollection(collection));
    }

    public RunnableActuator isNotEmptyCollection(Collection<?> collection){
        return isFalse(ContainerUtils.isEmptyCollection(collection));
    }

    public RunnableActuator isEmptyMap(Map<?,?> map){
        return isTrue(ContainerUtils.isEmptyMap(map));
    }

    public RunnableActuator isNotEmptyMap(Map<?,?> map){
        return isFalse(ContainerUtils.isEmptyMap(map));
    }

    public RunnableActuator isAssignableFrom(Class<?> baseClass, Class<?> implementClass){
        return isTrue(baseClass.isAssignableFrom(implementClass));
    }

    public RunnableActuator isNotAssignableFrom(Class<?> baseClass, Class<?> implementClass){
        return isFalse(baseClass.isAssignableFrom(implementClass));
    }

    public RunnableActuator isAnnotated(AnnotatedTypeMetadata typeMetadata, String annotationName){
        return isTrue(typeMetadata.isAnnotated(annotationName));
    }

    public RunnableActuator isNotAnnotated(AnnotatedTypeMetadata typeMetadata, String annotationName){
        return isFalse(typeMetadata.isAnnotated(annotationName));
    }

    public RunnableActuator isAnnotated(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass){
        return isTrue(AnnotationUtils.isAnnotated(annotatedElement, annotationClass));
    }

    public RunnableActuator isNotAnnotated(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass){
        return isFalse(AnnotationUtils.isAnnotated(annotatedElement, annotationClass));
    }


    public void run(Runnable runnable){
        if(isRunning){
            runnable.run();
        }
    }
}
