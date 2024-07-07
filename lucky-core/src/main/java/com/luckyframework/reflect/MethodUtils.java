package com.luckyframework.reflect;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class MethodUtils {

    private static final Logger log = LoggerFactory.getLogger(MethodUtils.class);
    private static final Method[] objectMethods = Object.class.getDeclaredMethods();

    /**
     * 判断方法是否为Object类的方法
     *
     * @param method 待判断的方法
     * @return
     */
    public static boolean isObjectMethod(Method method) {
        for (Method objectMethod : objectMethods) {
            if (method.equals(objectMethod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用反射机制执行方法
     *
     * @param targetObject 对象实例
     * @param method       要执行的方法的Method
     * @param params       方法执行所需要的参数
     * @return
     */
    public static Object invoke(Object targetObject, Method method, Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(targetObject, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LuckyReflectionException(e);
        }
    }

    public static Object invokeDefault(Object proxy, Method method, Object... args) {
        try {
            final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                    .getDeclaredConstructor(Class.class, int.class);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            final Class<?> declaringClass = method.getDeclaringClass();
            return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                    .unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
        } catch (Throwable e) {
            throw new LuckyReflectionException(e);
        }

    }

    /**
     * 使用反射机制执行方法
     *
     * @param targetObject 对象实例
     * @param methodName   要执行的方法的方法名
     * @param params       方法执行所需要的参数
     * @return
     */
    public static Object invoke(Object targetObject, String methodName, Object... params) {
        try {
            Method method = getTargetClass(targetObject).getMethod(methodName, ClassUtils.array2Class(params));
            return invoke(targetObject, method, params);
        } catch (NoSuchMethodException e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 使用反射机制执行方法
     *
     * @param targetObject       对象实例
     * @param declaredMethodName 要执行的方法的方法名
     * @param params             方法执行所需要的参数
     * @return
     */
    public static Object invokeDeclaredMethod(Object targetObject, String declaredMethodName, Object... params) {
        try {
            Method method = getTargetClass(targetObject).getDeclaredMethod(declaredMethodName, ClassUtils.array2Class(params));
            return invoke(targetObject, method, params);
        } catch (NoSuchMethodException e) {
            throw new LuckyReflectionException(e);
        }
    }

    private static Class<?> getTargetClass(Object targetObject) {
        return targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass();
    }

    /**
     * interface Method : public void method(String name,Double price)
     * ->
     * List[name,price]
     * 只有在编译参数-parameters开启后才能生效
     * <p>
     * 通过ASM得到接口中方法的所有参数名
     *
     * @param method 要操作的方法的Method
     * @return
     * @throws IOException
     */
    public static List<String> getInterfaceParamNames(Method method) throws IOException {
        return ASMUtil.getInterfaceMethodParamNames(method);
    }

    /**
     * class Method : public void method(String name,Double price)
     * ->
     * String[] =[name,price]
     * 不依赖编译参数-parameters
     * <p>
     * 通过ASM得到类方法的所有参数名
     *
     * @param method 要操作的方法的Method
     * @return
     */
    public static String[] getClassParamNames(Method method) {
        return ASMUtil.getMethodParamNames(method);
    }

    /**
     * 通过JDK8的Parameter类得到方法的所有参数名
     *
     * @param method 要操作的方法的Method
     * @return
     */
    public static String[] getParamNamesByParameter(Method method) {
        Parameter[] parameters = method.getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            names[i] = parameters[i].getName();
        return names;
    }

//    /**
//     * 将String类型的参数列表转化为Method方法执行时的Object类型的参数列表
//     * @param method 要操作的Method
//     * @param StrParam String类型的方法参数列表
//     * @return
//     */
//    public static Object[] getRunParam(Method method,String[] StrParam){
//        Parameter[] parameters = method.getParameters();
//        if(parameters.length!=StrParam.length){
//            throw new LuckyReflectionException("@InitRun参数错误(runParam提供的参数个数与方法参数列表个数不匹配--[ERROR m:"+parameters.length+" , p:"+StrParam.length+"])，位置："+method);
//        }
//        Object[] runParams=new Object[parameters.length];
//        for (int i = 0; i < parameters.length; i++) {
//            if(StrParam[i].startsWith("ref:")){
//                runParams[i]= ApplicationBeans.createApplicationBeans().getBean(StrParam[i].substring(4));
//            }else{
//                runParams[i]= JavaConversion.strToBasic(StrParam[i],parameters[i].getType());
//            }
//        }
//        return runParams;
//    }

    /**
     * 接口方法
     * 得到由参数列表的参数名和参数值所组成的Map
     *
     * @param method 接口方法
     * @param params 执行参数
     * @return
     * @throws IOException
     */
    public static Map<String, Object> getInterfaceMethodParamsNV(Method method, Object[] params) throws IOException {
        Map<String, Object> paramMap = new LinkedHashMap<>();
        List<String> interParams = ASMUtil.getInterfaceMethodParamNames(method);
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < interParams.size(); i++) {
            paramMap.put(ParameterUtils.getParamName(parameters[i], interParams.get(i)), params[i]);
        }
        return paramMap;
    }

    /**
     * 类方法
     * 得到由参数列表的参数名和参数值所组成的Map
     *
     * @param method 类方法
     * @param params 执行参数
     * @return
     * @throws IOException
     */
    public static Map<String, Object> getClassMethodParamsNV(Method method, Object[] params) throws IOException {
        Map<String, Object> paramMap = new LinkedHashMap<>();
        String[] mparams = ASMUtil.getMethodParamNames(method);
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            paramMap.put(ParameterUtils.getParamName(parameters[i], mparams[i]), params[i]);
        }
        return paramMap;
    }

    /**
     * 兼容接口方法与类方法
     * 得到由参数列表的参数名和参数值所组成的Map
     *
     * @param method 方法
     * @param params 执行参数
     * @return
     * @throws IOException
     */
    public static Map<String, Object> getMethodParamsNV(Method method, Object[] params) {
        try {
            Map<String, Object> interfaceMethodParamsNV = getInterfaceMethodParamsNV(method, params);
            return interfaceMethodParamsNV.isEmpty() ? getClassMethodParamsNV(method, params) : interfaceMethodParamsNV;
        } catch (IOException e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 获取方法返回值类型
     *
     * @param method 要操作的Method
     * @return
     */
    public static Class<?> getReturnType(Method method) {
        return method.getReturnType();
    }

    /**
     * 获取方法返回值类型的泛型，如果返回值不包含泛型则返回null
     *
     * @param method 要操作的Method
     * @return
     */
    public static Class<?>[] getReturnTypeGeneric(Method method) {
        Type type = method.getGenericReturnType();
        return ClassUtils.getGenericType(type);
    }

    public static Method getMethod(Class<?> targetClass, String methodName, Class<?>... paramClasses) {
        try {
            return targetClass.getMethod(methodName, paramClasses);
        } catch (NoSuchMethodException e) {
            throw new LuckyReflectionException(e);
        }
    }

    public static Method getDeclaredMethod(Class<?> targetClass, String methodName, Class<?>... paramClasses) {
        try {
            return targetClass.getDeclaredMethod(methodName, paramClasses);
        } catch (NoSuchMethodException e) {
            throw new LuckyReflectionException(e);
        }
    }

    public static Method[] getMethods(Class<?> targetClass) {
        return targetClass.getMethods();
    }

    public static Method[] getDeclaredMethods(Class<?> targetClass) {
        return targetClass.getDeclaredMethods();
    }

    public static Parameter[] getParameter(Method method) {
        return method.getParameters();
    }

    public static String getWithParamMethodName(Method method) {
        String methodName = method.getName();
        if (method.getParameterCount() == 0) {
            return methodName + "()";
        }
        Type[] parameterTypes = method.getGenericParameterTypes();
        List<String> paramTypeNames = new ArrayList<>(parameterTypes.length);
        for (Type parameterType : parameterTypes) {
            paramTypeNames.add(getClassSimpleName(ResolvableType.forType(parameterType)));
        }
        return StringUtils.join(paramTypeNames, methodName + "(", ", ", ")");
    }

    private static String getClassSimpleName(ResolvableType type) {
        String simpleName = Objects.requireNonNull(type.getRawClass()).getSimpleName();
        if (!type.hasGenerics()) {
            return simpleName;
        }
        ResolvableType[] generics = type.getGenerics();
        List<String> genericsSimpleNames = new ArrayList<>(generics.length);
        for (ResolvableType generic : generics) {
            genericsSimpleNames.add(getClassSimpleName(generic));
        }
        return StringUtils.join(genericsSimpleNames, simpleName + "<", ", ", ">");
    }
}
