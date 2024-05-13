package com.luckyframework.conversion;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.reflect.ParameterUtils;
import com.luckyframework.spel.SpELImport;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 转化代理拦截器，用于生成基于反射以及SpEL表达式实现的类型转化功能
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/3 04:25
 */
@SuppressWarnings("all")
public class ConversionInvocationHandler implements InvocationHandler {

    private ConvesionProxyCreator convesionProxyCreator;
    private SpELRuntime spELRuntime;

    public ConversionInvocationHandler(SpELRuntime spELRuntime, Class<?> conversionInterfaceClass){
        this.spELRuntime = spELRuntime == null ? new SpELRuntime() : spELRuntime;
        if(AnnotationUtils.isAnnotated(conversionInterfaceClass, SpELImport.class))  {
            this.spELRuntime.getCommonParams().importPackage(AnnotationUtils.findMergedAnnotation(conversionInterfaceClass, SpELImport.class).packages());
        }

        if(Interconversion.class.isAssignableFrom(conversionInterfaceClass)){
            convesionProxyCreator = new InterconversionProxyCreator((Class<? extends Interconversion>) conversionInterfaceClass);
        }else{
            convesionProxyCreator = new OrdinaryInterfaceConversionProxyCreator(conversionInterfaceClass);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return MethodUtils.invokeDefault(proxy, method, args);
        }
        if (ReflectionUtils.isEqualsMethod(method)) {
            return Objects.equals(proxy, args[0]);
        }
        if (ReflectionUtils.isHashCodeMethod(method)) {
            return proxy.getClass().hashCode();
        }
        if (ReflectionUtils.isToStringMethod(method)) {
            return convesionProxyCreator.getConversionInterfaceClass().getName() + proxy.getClass().getSimpleName();
        }
        if (MethodUtils.isObjectMethod(method)) {
            return MethodUtils.invoke(proxy, method, args);
        }
        return convesionProxyCreator.invokeMethodProxy(proxy, method, args);
    }


    /**
     * 转换器构造器
     */
    interface ConvesionProxyCreator {
        Object invokeMethodProxy(Object proxy, Method method, Object[] args) throws Throwable;
        Class<?> getConversionInterfaceClass();
    }

    /**
     * 基于普通接口实现的转化器构造器
     */
    class OrdinaryInterfaceConversionProxyCreator implements ConvesionProxyCreator {

        private final Class<?> conversionInterfaceClass;
        private final Set<Method> completeTheUseMethods = new HashSet<>();

        public OrdinaryInterfaceConversionProxyCreator(Class<?> conversionInterfaceClass){
            this.conversionInterfaceClass = conversionInterfaceClass;
            createAndRegistryConversionService();
        }

        private void createAndRegistryConversionService() {
            List<Method> conversionMethods = ClassUtils.getMethodByStrengthenAnnotation(conversionInterfaceClass, ConversionMethod.class);
            for (Method method : conversionMethods) {
                String conversionName = AnnotationUtils.findMergedAnnotation(method, ConversionMethod.class).name();
                conversionName = StringUtils.hasText(conversionName) ? conversionName : method.getName();
                SpELConversion conversion = ConversionAnnotationUtils.createConversion(conversionInterfaceClass, method);
                conversion.setSpELRunTime(spELRuntime);
                if(AnnotationUtils.isAnnotated(method, SpELImport.class)){
                    conversion.importPackages(AnnotationUtils.findMergedAnnotation(method, SpELImport.class).packages());
                }
                ConversionManager.registryConversionService(conversionName, conversion);
            }
        }

        @Override
        public Object invokeMethodProxy(Object proxy, Method method, Object[] args) throws Throwable {
            ConversionMethod conversionMethodAnn = AnnotationUtils.findMergedAnnotation(method, ConversionMethod.class);
            if(conversionMethodAnn != null){
                if(args != null){
                    // 一个参数的方法，使用SpELConversion转换器进行转换
                    if(args.length == 1){
                        String converionName = StringUtils.hasText(conversionMethodAnn.name()) ? conversionMethodAnn.name() : method.getName();
                        ConversionService conversionService = ConversionManager.getConversionService(converionName);
                        if(conversionService instanceof SpELConversion){
                            if(!completeTheUseMethods.contains(method)){
                                SpELConversion spELConversion = (SpELConversion) conversionService;
                                spELConversion.addConversions(ConversionAnnotationUtils.getUseConversionService(spELRuntime, method));
                                spELConversion.addConversions(ConversionAnnotationUtils.getUseConversionService(spELRuntime, conversionInterfaceClass));
                                completeTheUseMethods.add(method);
                            }
                        }
                        return ConversionUtils.conversion(args[0], ResolvableType.forMethodReturnType(method), conversionService);
                    }
                    // 多个参数的方法，使用PolymerizeConversion转换器进行转化
                    else if(args.length > 1){
                        ResolvableType methodReturnType = ResolvableType.forMethodReturnType(method);
                        PolymerizeConversion polyConversion = new PolymerizeConversion(ConversionService.getConversionClass(methodReturnType));
                        if(AnnotationUtils.isAnnotated(method, SpELImport.class)){
                            polyConversion.importPackages(AnnotationUtils.findMergedAnnotation(method, SpELImport.class).packages()) ;
                        }
                        polyConversion.setSpELRunTime(spELRuntime);

                        // 注册原对象
                        Parameter[] parameters = method.getParameters();
                        // 参数名注册
                        for (int i = 0; i < parameters.length; i++) {
                            // 参数名注册
                            polyConversion.addSourceObject(ParameterUtils.getParamName(parameters[i], parameters[i].getName()), args[i]);
                            // 参数索引注册
                            polyConversion.addSourceObject("p"+i, args[i]);
                            polyConversion.addSourceObject("a"+i, args[i]);
                        }

                        // 添加映射关系
                        polyConversion.addMappings(ConversionAnnotationUtils.getMapping(method));

                        return ConversionUtils.conversion(polyConversion.polymerize(), methodReturnType);

                    }

                }
            }
            return null;
        }

        @Override
        public Class<?> getConversionInterfaceClass() {
            return this.conversionInterfaceClass;
        }
    }

    /**
     * 基于{@link Interconversion}接口实现的的转换器构造器
     */
    class InterconversionProxyCreator implements ConvesionProxyCreator {

        /** Target类型*/
        private final ResolvableType intrefaceGenericTargetType;
        /** Source类型*/
        private final ResolvableType intrefaceGenericSourceType;
        /** 可以实现将Source类型转化为Target的转换器*/
        private final SpELConversion toTargetConversion;
        /** 可以实现将Target类型转化为Source的转换器*/
        private final SpELConversion toSourceConversion;
        /** 需要被代理的接口Class*/
        private final Class<? extends Interconversion> conversionInterfaceClass;

        /** Target转换器是否已经导入其他转换器了*/
        private boolean targetConversionUseComplete = false;
        /** Source转换器是否已经导入其他转换器了*/
        private boolean sourceConversionUseComplete = false;

        /** toSource方法*/
        private Method toSourceMethod;
        /** toTarget方法*/
        private Method toTargetMethod;

        private String[] toSourceSpELImport;
        private String[] toTargetSpELImport;

        public InterconversionProxyCreator(Class<? extends Interconversion> conversionInterfaceClass){
            ResolvableType conversionInterfaceType = ResolvableType.forClass(Interconversion.class, conversionInterfaceClass);
            this.intrefaceGenericTargetType = conversionInterfaceType.getGeneric(0);
            this.intrefaceGenericSourceType = conversionInterfaceType.getGeneric(1);

            Class<?> targetClass = ConversionService.getConversionClass(intrefaceGenericTargetType);
            Class<?> sourceClass = ConversionService.getConversionClass(intrefaceGenericSourceType);

            this.conversionInterfaceClass = conversionInterfaceClass;
            this.toSourceMethod = getToSourceMethod();
            this.toTargetMethod = getToTargetMethod();
            this.toTargetConversion = ConversionAnnotationUtils.createConversion(conversionInterfaceClass, toTargetMethod);
            this.toSourceConversion = ConversionAnnotationUtils.createConversion(conversionInterfaceClass, toSourceMethod);

            this.toTargetConversion.setSpELRunTime(spELRuntime);
            this.toTargetConversion.setSpELRunTime(spELRuntime);

            if(AnnotationUtils.isAnnotated(toTargetMethod, SpELImport.class)){
                this.toTargetSpELImport = AnnotationUtils.findMergedAnnotation(toTargetMethod, SpELImport.class).packages();
            }

            if(AnnotationUtils.isAnnotated(toSourceMethod, SpELImport.class)){
                 this.toSourceSpELImport = AnnotationUtils.findMergedAnnotation(toSourceMethod, SpELImport.class).packages();
            }
            ConversionAnnotationUtils.tryToRegisterConvert(toSourceMethod, toSourceConversion);
            ConversionAnnotationUtils.tryToRegisterConvert(toTargetMethod, toTargetConversion);
        }

        @Override
        public Object invokeMethodProxy(Object proxy, Method method, Object[] args) throws Throwable {
            if(args == null){
                switch (method.getName()){
                    case Interconversion.GET_SOURCE_CONVERT : return toSourceConversion;
                    case Interconversion.GET_TARGET_CONVERT : return toTargetConversion;
                    default                                 : return null;
                }
            }
            if(args.length == 1){
                Object conversionObject = args[0];
                switch (method.getName()){
                    case Interconversion.TO_SOURCE      : {
                        sourceConversionUse(method);
                        return ConversionUtils.conversion(conversionObject, intrefaceGenericSourceType, toSourceConversion);
                    }
                    case Interconversion.TO_TARGET      : {
                        targetConversionUse(method);
                        return ConversionUtils.conversion(conversionObject, intrefaceGenericTargetType, toTargetConversion);
                    }
                    case Interconversion.TO_TARGET_LIST : {
                        targetConversionUse(toTargetMethod);
                        return ConversionUtils.conversion(conversionObject, ResolvableType.forClassWithGenerics(List.class, intrefaceGenericTargetType), toTargetConversion);
                    }
                    case Interconversion.TO_SOURCE_LIST : {
                        sourceConversionUse(toSourceMethod);
                        return ConversionUtils.conversion(conversionObject, ResolvableType.forClassWithGenerics(List.class, intrefaceGenericSourceType), toSourceConversion);
                    }
                    case Interconversion.TO_TARGET_SET : {
                        targetConversionUse(toTargetMethod);
                        return ConversionUtils.conversion(conversionObject, ResolvableType.forClassWithGenerics(Set.class, intrefaceGenericTargetType), toTargetConversion);
                    }
                    case Interconversion.TO_SOURCE_SET : {
                        sourceConversionUse(toSourceMethod);
                        return ConversionUtils.conversion(conversionObject, ResolvableType.forClassWithGenerics(Set.class, intrefaceGenericSourceType), toSourceConversion);
                    }
                    case Interconversion.TO_TARGET_ARRAY : {
                        targetConversionUse(toTargetMethod);
                        return ConversionUtils.conversion(conversionObject, ResolvableType.forArrayComponent(intrefaceGenericTargetType), toTargetConversion);
                    }
                    case Interconversion.TO_SOURCE_ARRAY : {
                        sourceConversionUse(toSourceMethod);
                        return ConversionUtils.conversion(conversionObject, ResolvableType.forArrayComponent(intrefaceGenericSourceType), toSourceConversion);
                    }
                    default: return null;
                }
            }
            return null;
        }

        @Override
        public Class<?> getConversionInterfaceClass() {
            return this.conversionInterfaceClass;
        }

        /**
         * 获取toSource方法的实例
         * @return toSource方法的实例
         */
        private Method getToSourceMethod(){
            return MethodUtils.getDeclaredMethod(conversionInterfaceClass, Interconversion.TO_SOURCE, intrefaceGenericTargetType.getRawClass());
        }

        /**
         * 获取toTarget方法的实例
         * @return toTarget方法的实例
         */
        private Method getToTargetMethod(){
            return MethodUtils.getDeclaredMethod(conversionInterfaceClass, Interconversion.TO_TARGET, intrefaceGenericSourceType.getRawClass());
        }

        /**
         * 给Target转化器导入其他转换器
         * @param toTargetMethod toTarget方法实例
         */
        private void targetConversionUse(Method toTargetMethod){
            if(targetConversionUseComplete == false){
                toTargetConversion.addConversions(ConversionAnnotationUtils.getUseConversionService(spELRuntime, toTargetMethod));
                toTargetConversion.addConversions(ConversionAnnotationUtils.getUseConversionService(spELRuntime, conversionInterfaceClass));
                targetConversionUseComplete = true;
            }
        }

        /**
         * 给Source转化器导入其他转换器
         * @param toSourceMethod toSource方法实例
         */
        private void sourceConversionUse(Method toSourceMethod){
            if(sourceConversionUseComplete == false){
                toSourceConversion.addConversions(ConversionAnnotationUtils.getUseConversionService(spELRuntime, toSourceMethod));
                toSourceConversion.addConversions(ConversionAnnotationUtils.getUseConversionService(spELRuntime, conversionInterfaceClass));
                sourceConversionUseComplete = true;
            }
        }
    }

}
