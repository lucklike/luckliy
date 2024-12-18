package com.luckyframework.reflect;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.exception.LuckyReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.util.ClassUtils.getAllInterfacesForClassAsSet;

@SuppressWarnings("unchecked")
public abstract class ClassUtils {

    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);

    //简单类型的包装类型
    public static final Class<?>[] SIMPLE_CLASSES = {
            String.class, Byte.class,
            Short.class, Integer.class,
            Long.class, Float.class,
            Double.class, Boolean.class,
            BigDecimal.class, BigInteger.class,
            Number.class, Date.class
    };

    //可进行运算的类型
    public static final Class<?>[] CAN_CALCULATED_TYPE = {
            char.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class,
            Boolean.class, boolean.class,
            BigDecimal.class, BigInteger.class,
            Number.class
    };

    /**
     * 使用全类名获取一个Class对象
     *
     * @param fullPath 全类名
     * @param loader   类加载器
     * @return Class对象
     */
    public static Class<?> forName(String fullPath, ClassLoader loader) {
        Assert.notNull(fullPath, "Name must not be null");
        try {
            return Class.forName(fullPath, true, loader);
        } catch (ClassNotFoundException e) {
            throw new LuckyReflectionException(e, "To find the corresponding class for '{}'", fullPath);
        }
    }

    public static Field[] getAllFieldsOrder(Class<?> clazz) {
        return Stream.of(getAllFields(clazz)).sorted((f1, f2) -> {
            int p1 = AnnotationUtils.isAnnotated(f1, Order.class) ? f1.getAnnotation(Order.class).value() : Integer.MAX_VALUE;
            int p2 = AnnotationUtils.isAnnotated(f2, Order.class) ? f2.getAnnotation(Order.class).value() : Integer.MAX_VALUE;
            return Integer.compare(p1, p2);
        }).toArray(Field[]::new);
    }

    /**
     * 获取一个类中所有属性实例，通过继承结构向上找，直到Object类结束
     *
     * @param aClass 目标类的Class
     * @return 类中所有属性实例
     */
    public static Field[] getAllFields(Class<?> aClass) {
        if (aClass == null) {
            return new Field[0];
        }
        if (aClass.getSuperclass() == Object.class) {
            return aClass.getDeclaredFields();
        }
        Field[] currentFields = aClass.getDeclaredFields();

        List<Field[]> supersFields = new ArrayList<>();
        for (Class<?> anInterface : aClass.getInterfaces()) {
            supersFields.add(getAllFields(anInterface));
        }
        supersFields.add(getAllFields(aClass.getSuperclass()));
        return delCoverFields(currentFields, supersFields);
    }

    public static Method[] getAllMethodOrder(Class<?> aClass) {
        return Stream.of(getAllMethod(aClass)).sorted((m1, m2) -> {
            int p1 = AnnotationUtils.isAnnotated(m1, Order.class) ? m1.getAnnotation(Order.class).value() : Integer.MAX_VALUE;
            int p2 = AnnotationUtils.isAnnotated(m2, Order.class) ? m2.getAnnotation(Order.class).value() : Integer.MAX_VALUE;
            return Integer.compare(p1, p2);
        }).toArray(Method[]::new);
    }

    /**
     * 获取一个类中所有方法实例，通过继承结构向上找，直到Object类结束
     *
     * @param aClass 目标类的Class
     * @return 类中所有方法实例
     */
    public static Method[] getAllMethod(Class<?> aClass) {
        if (aClass == null) {
            return new Method[0];
        }
        if (aClass.getSuperclass() == Object.class) {
            return aClass.getDeclaredMethods();
        }
        Method[] currentMethods = aClass.getDeclaredMethods();

        List<Method[]> supersMethods = new ArrayList<>();
        for (Class<?> anInterface : aClass.getInterfaces()) {
            supersMethods.add(getAllMethod(anInterface));
        }
        supersMethods.add(getAllMethod(aClass.getSuperclass()));
        return delCoverMethods(currentMethods, supersMethods);
    }

    /**
     * 获取一个类中所有属性名和属性实例所组成的Map,属性的查找基于{@link #getAllFields(Class)}实现
     *
     * @param aClass 目标Class
     * @return 类中所有属性名和属性实例所组成的Map
     */
    public static Map<String, Field> getNameFieldMap(Class<?> aClass) {
        Map<String, Field> nameFieldMap = new LinkedHashMap<>();
        Field[] allFields = getAllFields(aClass);
        if (!ContainerUtils.isEmptyArray(allFields)) {
            for (Field field : allFields) {
                nameFieldMap.put(field.getName(), field);
            }
        }
        return nameFieldMap;
    }

    /**
     * 获取一个类中所有的get方法实例，方法查找基于{@link #getAllMethod(Class)} 实现
     *
     * @param aClass 目标Class
     * @return 类中所有的get方法实例
     */
    public static Method[] getAllGetterMethods(Class<?> aClass) {
        return Stream.of(getAllMethod(aClass))
                .filter(ClassUtils::getterMethodNameCheck)
                .toArray(Method[]::new);
    }

    /**
     * 获取一个类中所有get方法名称和其实例所组成的Map，方法查找基于{@link #getAllMethod(Class)} 实现
     *
     * @param aClass 目标Class
     * @return 类中所有get方法名称和其实例所组成的Map
     */
    public static Map<String, Method> getAllGetterMethodMap(Class<?> aClass) {
        Map<String, Method> getterMethodMap = new LinkedHashMap<>();
        for (Method getterMethod : getAllGetterMethods(aClass)) {
            getterMethodMap.put(getterMethod.getName(), getterMethod);
        }
        return getterMethodMap;
    }

    private static boolean getterMethodNameCheck(Method method) {
        if (method.getParameterCount() != 0) {
            return false;
        }

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            return false;
        }

        String getterMethodPrefix = returnType == boolean.class ? "is" : "get";
        String methodName = method.getName();

        if (methodName.length() <= getterMethodPrefix.length()) {
            return false;
        }
        if (!methodName.startsWith(getterMethodPrefix)) {
            return false;
        }
        String setterMethodSuffixFirst = methodName.substring(getterMethodPrefix.length());
        return Character.isUpperCase(setterMethodSuffixFirst.charAt(0));
    }

    public static Map<String, Method> getAllSetterMethodMap(Class<?> aClass) {
        Map<String, Method> setterMethodMap = new LinkedHashMap<>();
        for (Method setterMethod : getAllSetterMethods(aClass)) {
            setterMethodMap.put(setterMethod.getName(), setterMethod);
        }
        return setterMethodMap;
    }

    public static Method[] getAllSetterMethods(Class<?> aClass) {
        return Stream.of(getAllMethod(aClass))
                .filter(ClassUtils::setterMethodNameCheck)
                .toArray(Method[]::new);
    }

    private static boolean setterMethodNameCheck(Method method) {
        if (method.getParameterCount() != 1) {
            return false;
        }
        String methodName = method.getName();
        String setterMethodPrefix = "set";
        if (methodName.length() <= setterMethodPrefix.length()) {
            return false;
        }
        if (!methodName.startsWith(setterMethodPrefix)) {
            return false;
        }
        String setterMethodSuffixFirst = methodName.substring(setterMethodPrefix.length());
        return Character.isUpperCase(setterMethodSuffixFirst.charAt(0));
    }


    public static <T> Constructor<T> getConstructor(Class<T> aClass, Class<?>[] paramTypes) {
        try {
            return aClass.getConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            throw new LuckyReflectionException(e, "An exception occurred while obtaining the '{}' class constructor. The parameter list currently in use is of type :{}", aClass, Arrays.toString(paramTypes));
        }
    }

    /**
     * 过滤掉被@Cover注解标注的属性
     *
     * @param thisFields   当前类的所有属性
     * @param supersFields 当前类父类的所有属性
     * @return
     */
    private static Field[] delCoverFields(Field[] thisFields, List<Field[]> supersFields) {
        List<Field> delCvoerFields = new ArrayList<>();
        Set<String> coverFieldNames = Stream.of(thisFields)
                .filter(f -> AnnotationUtils.isAnnotated(f, Cover.class))
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (Field[] superFields : supersFields) {
            for (Field superField : superFields) {
                if (!coverFieldNames.contains(superField.getName())) {
                    delCvoerFields.add(superField);
                }
            }
        }
        delCvoerFields.addAll(Arrays.asList(thisFields));
        return delCvoerFields.toArray(new Field[0]);
    }

    public static List<Field> getAllStaticFieldOrder(Class<?> aClass) {
        return Arrays.stream(getAllFieldsOrder(aClass))
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .collect(Collectors.toList());
    }

    public static List<Field> getAllStaticField(Class<?> aClass) {
        return Arrays.stream(getAllFields(aClass))
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .collect(Collectors.toList());
    }

    public static List<Method> getAllStaticMethodOrder(Class<?> aClass) {
        return Arrays.stream(getAllMethodOrder(aClass))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .collect(Collectors.toList());
    }

    public static List<Method> getAllStaticMethod(Class<?> aClass) {
        return Arrays.stream(getAllMethod(aClass))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .collect(Collectors.toList());
    }

    public static List<Method> getAllStaticMethod(Class<?> aClass, String staticMethodName) {
        List<Method> staticMethodList = new ArrayList<>();
        List<Method> allStaticMethod = getAllStaticMethod(aClass);
        for (Method method : allStaticMethod) {
            if (method.getName().equals(staticMethodName)) {
                staticMethodList.add(method);
            }
        }
        return staticMethodList;
    }

    public static List<Method> getAllMethod(Class<?> aClass, String methodName) {
        List<Method> methodList = new ArrayList<>();
        Method[] allMethod = getAllMethod(aClass);
        for (Method method : allMethod) {
            if (method.getName().equals(methodName)) {
                methodList.add(method);
            }
        }
        return methodList;
    }

    public static Method getMethod(Class<?> aClass, String methodName) {
        Method[] allMethod = getAllMethod(aClass);
        for (Method method : allMethod) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new LuckyReflectionException("No method named '{}' can be found in class '{}'", methodName, aClass);
    }

    public static Field getField(Class<?> aClass, String fieldName) {
        Field[] allFields = getAllFields(aClass);
        for (Field field : allFields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        throw new LuckyReflectionException("The attribute named '{}' was not found in class '{}'", fieldName, aClass);
    }

    /**
     * 过滤掉被@Cover注解标注的方法
     *
     * @param thisMethods   当前类的所有方法
     * @param supersMethods 当前类父类的所有方法
     * @return
     */
    private static Method[] delCoverMethods(Method[] thisMethods, List<Method[]> supersMethods) {
        List<Method> delCoverMethods = new ArrayList<>();
        Set<String> coverMethodNames = Stream.of(thisMethods)
                .filter(m -> AnnotationUtils.isAnnotated(m, Cover.class))
                .map(Method::toGenericString)
                .collect(Collectors.toSet());

        for (Method[] superMethods : supersMethods) {
            for (Method superMethod : superMethods) {
                if (!coverMethodNames.contains(superMethod.toGenericString())) {
                    delCoverMethods.add(superMethod);
                }
            }
        }
        delCoverMethods.addAll(Arrays.asList(thisMethods));
        return delCoverMethods.toArray(new Method[0]);
    }

    /**
     * 使用反射机制调用构造函数创建一个对象
     *
     * @param tClass 目标对象的Class
     * @param args   构造器执行的参数
     * @param <T>    实例类型
     * @return 类实例
     */
    public static <T> T newObject(Class<T> tClass, Object... args) {
        try {
            Constructor<T> constructor = findConstructor(tClass, array2Class(args));
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new LuckyReflectionException(e, "An exception occurred while creating an instance of the '{}' class using reflection. The construct parameter currently used is: {}", tClass, Arrays.toString(args));
        }
    }

    /**
     * 使用构造器创建一个实例对象
     *
     * @param constructor 构造器
     * @param args        参数列表
     * @param <T>         实例类型
     * @return 类实例
     */
    public static <T> T newObject(Constructor<T> constructor, Object... args) {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new LuckyReflectionException(e, "An exception occurred while creating a class instance with the constructor '{}'. The current execution parameter is: {}", constructor, Arrays.toString(args));
        }
    }

    public static boolean isStaticMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isStatic(modifiers);
    }

    public static boolean isPublicMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers);
    }

    public static Method findStaticMethod(Class<?> tClass, String methodName, Class<?>[] methodParamClasses) {
        try {
            Method method = tClass.getMethod(methodName, methodParamClasses);
            if (isPublicMethod(method) && isStaticMethod(method)) {
                return method;
            }
        } catch (Exception ignored) {

        }
        List<Method> methods = getAllMethods(tClass);
        out:
        for (Method m : methods) {
            if (!m.getName().equals(methodName) || !isStaticMethod(m) || !isPublicMethod(m)) {
                continue;
            }
            Class<?>[] parameterTypes = m.getParameterTypes();
            if (parameterTypes.length == methodParamClasses.length) {
                for (int i = 0, j = parameterTypes.length; i < j; i++) {
                    if (!parameterTypes[i].isAssignableFrom(methodParamClasses[i])) {
                        continue out;
                    }
                }
                return m;
            }
        }
        throw new LuckyReflectionException("There is no static factory method named '{}' parameter type '{}' in '{}'", methodName, Arrays.toString(methodParamClasses), tClass);
    }

    public static Method findStaticMethod(Class<?> tClass, String methodName, ResolvableType[] inputMethodParamResolvableTypes) {
        List<Method> methods = getAllMethods(tClass);
        out:
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || !isStaticMethod(method)) {
                continue;
            }
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            if (genericParameterTypes.length == inputMethodParamResolvableTypes.length) {
                for (int i = 0; i < genericParameterTypes.length; i++) {
                    ResolvableType paramResolvableType = ResolvableType.forType(genericParameterTypes[i]);
                    ResolvableType inputParamResolvableType = inputMethodParamResolvableTypes[i];
                    //一个有泛型一个没有泛型，跳过
                    if ((paramResolvableType.hasGenerics() && !inputParamResolvableType.hasGenerics()) ||
                            (!paramResolvableType.hasGenerics() && inputParamResolvableType.hasGenerics())) {
                        continue out;
                    }
                    //两个都有泛型
                    if (paramResolvableType.hasGenerics() && inputParamResolvableType.hasGenerics()) {
                        if (!paramResolvableType.toString().equals(inputParamResolvableType.toString())) {
                            continue out;
                        }
                    }
                    //两个都没有泛型
                    else {
                        Class<?> parameterClass = paramResolvableType.getRawClass();
                        Class<?> inputParamClass = inputParamResolvableType.getRawClass();
                        //两个类型即没有继承关系，也不是基本类型与包装类型的冠词
                        if (!parameterClass.isAssignableFrom(inputParamClass) && isWrapperType(parameterClass, inputParamClass)) {
                            continue out;
                        }
                    }
                }
                return method;
            }
        }
        throw new LuckyReflectionException("There is no static factory method named '{}' parameter type '{}' in '{}'", methodName, Arrays.toString(inputMethodParamResolvableTypes), tClass);
    }

    public static Method findMethod(Class<?> tClass, String methodName, Class<?>[] methodParamClasses) {
        try {
            return tClass.getMethod(methodName, methodParamClasses);
        } catch (Exception ignored) {

        }
        List<Method> methods = getAllMethods(tClass);
        out:
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == methodParamClasses.length) {
                for (int i = 0, j = parameterTypes.length; i < j; i++) {
                    if (!parameterTypes[i].isAssignableFrom(methodParamClasses[i])) {
                        continue out;
                    }
                }
                return method;
            }
        }
        throw new LuckyReflectionException("There is no static factory method named '{}' parameter type '{}' in '{}'", methodName, Arrays.toString(methodParamClasses), tClass);
    }

    public static Method findMethod(Class<?> tClass, String methodName, ResolvableType[] inputMethodParamResolvableTypes) {
        List<Method> methods = getAllMethods(tClass);
        out:
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            if (genericParameterTypes.length == inputMethodParamResolvableTypes.length) {
                for (int i = 0; i < genericParameterTypes.length; i++) {
                    ResolvableType paramResolvableType = ResolvableType.forType(genericParameterTypes[i]);
                    ResolvableType inputParamResolvableType = inputMethodParamResolvableTypes[i];
                    //一个有泛型一个没有泛型
                    if ((paramResolvableType.hasGenerics() && !inputParamResolvableType.hasGenerics()) ||
                            (!paramResolvableType.hasGenerics() && inputParamResolvableType.hasGenerics())) {
                        continue out;
                    }
                    //两个都有泛型
                    if (paramResolvableType.hasGenerics() && inputParamResolvableType.hasGenerics()) {
                        if (!paramResolvableType.toString().equals(inputParamResolvableType.toString())) {
                            continue out;
                        }
                    }
                    //两个都没有泛型
                    else {
                        Class<?> parameterClass = paramResolvableType.getRawClass();
                        Class<?> inputParamClass = inputParamResolvableType.getRawClass();
                        //两个类型即没有继承关系，也不是基本类型与包装类型的冠词
                        if (!parameterClass.isAssignableFrom(inputParamClass) && isWrapperType(parameterClass, inputParamClass)) {
                            continue out;
                        }
                    }
                }
                return method;
            }
        }
        throw new LuckyReflectionException("There is no factory method named '" + methodName + "' parameter type '" + Arrays.toString(inputMethodParamResolvableTypes) + "' in '" + tClass + "'");
    }

    /**
     * 判断两个类型是否为基本类型和包装类型的关系
     *
     * @param aClass1 类型1
     * @param aClass2 类型二
     * @return 两个类型是否为基本类型和包装类型的关系
     */
    public static boolean isWrapperType(Class<?> aClass1, Class<?> aClass2) {
        boolean primitive1 = aClass1.isPrimitive();
        boolean primitive2 = aClass2.isPrimitive();
        if ((primitive1 && !primitive2) || (!primitive1 && primitive2)) {
            if (primitive1) {
                return aClass1 != getBaseType(aClass2);
            } else {
                return aClass2 != getBaseType(aClass1);
            }
        } else {
            return true;
        }

    }

    public static Class<?> getBaseType(Class<?> wrapperClass) {
        try {
            Field type = wrapperClass.getField("TYPE");
            return (Class<?>) FieldUtils.getValue(null, type);
        } catch (Exception e) {
            return null;
        }

    }

    public static <T> Constructor<T> findConstructor(Class<T> tClass, Class<?>[] argsClasses) {
        Constructor<?> ct = null;
        try {
            ct = getConstructor(tClass, argsClasses);
        } catch (LuckyReflectionException ignored) {
            //使用参数的类型进行精确查找失败...
        }

        // 使用类型兼容性比较
        if (ct == null) {
            Constructor<?>[] constructors = tClass.getConstructors();
            out:
            for (Constructor<?> constructor : constructors) {
                int paramCount = constructor.getParameterCount();
                if (argsClasses.length == paramCount) {
                    for (int i = 0; i < paramCount; i++) {
                        if (!compatibleOrNot(ResolvableType.forConstructorParameter(constructor, i), ResolvableType.forType(argsClasses[i]))) {
                            continue out;
                        }
                    }
                    ct = constructor;
                    break;
                }
            }
        }

        if (ct == null) {
            throw new LuckyReflectionException("There is no corresponding construction method class = '{}' , args = '{}'", tClass, Arrays.toString(argsClasses));
        }

        return (Constructor<T>) ct;


    }

    public static <T> Constructor<T> findConstructor(Class<T> tClass, ResolvableType[] argsResolvableTypes) {
        Constructor<?>[] constructors = tClass.getConstructors();
        out:
        for (Constructor<?> constructor : constructors) {
            Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            if (genericParameterTypes.length == argsResolvableTypes.length) {
                for (int i = 0; i < genericParameterTypes.length; i++) {
                    ResolvableType paramResolvableType = ResolvableType.forType(genericParameterTypes[i]);
                    ResolvableType argsResolvableType = argsResolvableTypes[i];
                    //一个有泛型一个没有泛型
                    if ((paramResolvableType.hasGenerics() && !argsResolvableType.hasGenerics()) ||
                            (!paramResolvableType.hasGenerics() && argsResolvableType.hasGenerics())) {
                        continue out;
                    }
                    //两个都有泛型
                    if (paramResolvableType.hasGenerics() && argsResolvableType.hasGenerics()) {
                        if (!paramResolvableType.toString().equals(argsResolvableType.toString())) {
                            continue out;
                        }
                    }
                    //两个都没有泛型
                    else {
                        Class<?> parameterClass = paramResolvableType.getRawClass();
                        Class<?> inputParamClass = argsResolvableType.getRawClass();
                        //两个类型即没有继承关系，也不是基本类型与包装类型的关系
                        if (!parameterClass.isAssignableFrom(inputParamClass) && isWrapperType(parameterClass, inputParamClass)) {
                            continue out;
                        }
                    }
                }
                return (Constructor<T>) constructor;
            }
        }
        throw new LuckyReflectionException("There is no corresponding construction method class = '{}' , args = '{}'", tClass, Arrays.toString(argsResolvableTypes));

    }

    /**
     * 将一个Object[]转化为对应类型的Class[]
     *
     * @param objs 要操作的Object[]
     * @return
     */
    public static Class<?>[] array2Class(Object[] objs) {
        Class<?>[] paramsClass = new Class<?>[objs.length];
        for (int i = 0; i < objs.length; i++) {
            paramsClass[i] = objs[i].getClass();
        }
        return paramsClass;
    }

    /**
     * 得到某个泛型Type的所有泛型类型
     *
     * @param type 泛型Type
     * @return
     */
    public static Class<?>[] getGenericType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] types = pt.getActualTypeArguments();
            Class<?>[] genericType = new Class<?>[types.length];
            for (int i = 0; i < types.length; i++) {
                genericType[i] = (Class<?>) types[i];
            }
            return genericType;
        } else {
            return null;
        }
    }

    /**
     * 判断某个类型是否为JDK自带的类型
     *
     * @param clzz 目标类型
     * @return
     */
    public static boolean isJdkBasic(Class<?> clzz) {
        return clzz.getClassLoader() == null;
    }

    /**
     * 根据类的全路径得到一个Class
     *
     * @param className 类的全路径
     * @return
     */
    public static Class<?> getClass(String className) {
        return forName(className, getDefaultClassLoader());
    }

    public static Object newObject(String fullPath) {
        return newObject(getClass(fullPath));
    }

    public static Object newObject(String fullPath, Object... params) {
        return newObject(getClass(fullPath), params);
    }

    /**
     * 得到一个类中被特定注解标注的所有属性
     *
     * @param clzz       类CLass
     * @param annotation 注解类型
     * @return 被注解标注的所有Field
     */
    public static List<Field> getFieldByAnnotation(Class<?> clzz, Class<? extends Annotation> annotation) {
        Field[] allFields = getAllFields(clzz);
        List<Field> annFields = new ArrayList<>();
        for (Field field : allFields) {
            if (AnnotationUtils.isExist(field, annotation)) {
                annFields.add(field);
            }
        }
        return annFields;
    }

    public static List<Field> getFieldByAnnotationArrayOR(Class<?> clzz, Class<? extends Annotation>[] annotationArray) {
        Field[] allFields = getAllFields(clzz);
        List<Field> annFields = new ArrayList<>();
        for (Field field : allFields) {
            if (AnnotationUtils.isExistOrByArray(field, annotationArray)) {
                annFields.add(field);
            }
        }
        return annFields;
    }

    /**
     * 得到一个类中被特定注解标注的所有属性(包括注解中的组合注解)
     *
     * @param clzz       类CLass
     * @param annotation 注解类型
     * @return 被注解标注的所有Field
     */
    public static List<Field> getFieldByStrengthenAnnotation(Class<?> clzz, Class<? extends Annotation> annotation) {
        Field[] allFields = getAllFields(clzz);
        List<Field> annFields = new ArrayList<>();
        for (Field field : allFields) {
            if (AnnotationUtils.strengthenIsExist(field, annotation)) {
                annFields.add(field);
            }
        }
        return annFields;
    }


    /**
     * 得到一个类中被特定注解标注的所有方法
     *
     * @param clzz       类CLass
     * @param annotation 注解类型
     * @return 被注解标注的所有Method
     */
    public static List<Method> getMethodByAnnotation(Class<?> clzz, Class<? extends Annotation> annotation) {
        Method[] allMethods = getAllMethod(clzz);
        List<Method> annMethods = new ArrayList<>();
        for (Method method : allMethods) {
            if (AnnotationUtils.isExist(method, annotation)) {
                annMethods.add(method);
            }
        }
        return annMethods;
    }

    public static List<Method> getMethodByAnnotationArrayOR(Class<?> clzz, Class<? extends Annotation>[] annotationArray) {
        Method[] allMethod = getAllMethod(clzz);
        List<Method> annMethods = new ArrayList<>();
        for (Method method : allMethod) {
            if (AnnotationUtils.isExistOrByArray(method, annotationArray)) {
                annMethods.add(method);
            }
        }
        return annMethods;
    }

    /**
     * 得到一个类中被特定注解标注的所有方法(包括注解中的组合注解)
     *
     * @param clzz       类CLass
     * @param annotation 注解类型
     * @return 被注解标注的所有Method
     */
    public static List<Method> getMethodByStrengthenAnnotation(Class<?> clzz, Class<? extends Annotation> annotation) {
        Method[] allMethods = getAllMethod(clzz);
        List<Method> annMethods = new ArrayList<>();
        for (Method method : allMethods) {
            if (AnnotationUtils.strengthenIsExist(method, annotation)) {
                annMethods.add(method);
            }
        }
        return annMethods;
    }


    /**
     * 判断当前类型是否为Java基本类型
     *
     * @param aClass 当前类型
     * @return
     */
    public static boolean isPrimitive(Class<?> aClass) {
        return aClass.isPrimitive();
    }

    public static boolean isAbstract(Class<?> aClass) {
        int modifiers = aClass.getModifiers();
        return Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers);
    }

    /**
     * 判断当前类型是否为Java基本类型的包装类型
     *
     * @param aClass 当前类型
     * @return
     */
    public static boolean isSimple(Class<?> aClass) {
        for (Class<?> simpleClass : SIMPLE_CLASSES) {
            if (simpleClass.isAssignableFrom(aClass)) {
                return true;
            }
        }
        return false;
    }

    public static final Class<?>[] SIMPLE_ARRAY_CLASSES = {
            String[].class, Byte[].class, Short[].class, Integer[].class,
            Long[].class, Float[].class, Double[].class, Boolean[].class,
            char[].class, byte[].class, short[].class, int[].class,
            long[].class, float[].class, double[].class, boolean[].class
    };

    /**
     * 判断当前类型是否为Java基本类型的包装类型
     *
     * @param aClass 当前类型
     * @return
     */
    public static boolean isSimpleArray(Class<?> aClass) {
        for (Class<?> simpleClass : SIMPLE_ARRAY_CLASSES) {
            if (aClass == simpleClass) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAssignableFromArrayOr(Class<?> targetClass, Class<?>[] arrayClass) {
        for (Class<?> aClass : arrayClass) {
            if (aClass.isAssignableFrom(targetClass)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isJdkType(Class<?> aClass) {
        return aClass.getClassLoader() == null;
    }

    public static List<Method> getAllMethods(Class<?> aClass) {
        List<Method> allMethods = new LinkedList<>();
        //获取beanClass的所有接口
        Set<Class<?>> classes = new LinkedHashSet<>(getAllInterfacesForClassAsSet(aClass));
        classes.add(aClass);
        //遍历所有的类和接口反射获取到所有的方法
        for (Class<?> clazz : classes) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            allMethods.addAll(Arrays.asList(methods));
        }
        return allMethods;
    }

    /**
     * 判断传入的类型是否为Java基本数据类型或基本类型的包装类型
     *
     * @param aclass 带判断的类型
     * @return
     */
    public static boolean isSimpleBaseType(Class<?> aclass) {
        return isJdkBasic(aclass) && (isSimple(aclass) || isPrimitive(aclass));
    }

    /**
     * 判断传入的类型是否为可计算的类型
     *
     * @param aClass 带判断的类型
     * @return
     */
    public static boolean isCanCalculated(Class<?> aClass) {
        for (Class<?> canCalculatedType : CAN_CALCULATED_TYPE) {
            if (canCalculatedType == aClass) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ClassLoader getDefaultClassLoader() {
        return org.springframework.util.ClassUtils.getDefaultClassLoader();
    }

    public static boolean isJDKProxy(Object object) {
        return Proxy.isProxyClass(object.getClass());
    }

    public static boolean isJDKProxy(Class<?> objectClass) {
        return Proxy.isProxyClass(objectClass);
    }

    public static boolean isCglibProxy(Object object) {
        return isCglibProxy(object.getClass());
    }

    public static boolean isCglibProxy(Class<?> objectClass) {
        return objectClass.getName().contains("$$ByLuckyCGLIB$$");
    }

    public static Object getCglibTargetObject(Object proxyObject) {
        Field h = FieldUtils.getDeclaredField(proxyObject.getClass(), "CGLIB$CALLBACK_0");
        Object hv = FieldUtils.getValue(proxyObject, h);
        Field targetField = FieldUtils.getDeclaredField(hv.getClass(), "target");
        return FieldUtils.getValue(hv, targetField);
    }


    public static Object createObject(Class<?> aclass, Supplier<Object> defaultSupplier) {
        int modifiers = aclass.getModifiers();
        if (Modifier.isInterface(modifiers) || Modifier.isAbstract(aclass.getModifiers()) || aclass.isAnnotation()) {
            return defaultSupplier.get();
        }
        try {
            Constructor<?> constructor = aclass.getConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            return defaultSupplier.get();
        }
    }

    /**
     * 判断两个类型是否兼容
     *
     * @param baseType    基本类型
     * @param checkedType 待检测的类型
     * @return 基本类型是否兼容待检测的类型
     */
    public static boolean compatibleOrNot(ResolvableType baseType, ResolvableType checkedType) {

        // baseType.resolve() == null 表示泛型类型为 ?
        if (baseType.resolve() == null || baseType.resolve() == Object.class) {
            return true;
        }

        if (checkedType.resolve() == null || checkedType.resolve() == Object.class) {
            return false;
        }

        // 基本类型为数组时,需要比较元素类型（注：基本数据类型数组和其包装类的数组是不兼容的）
        if (baseType.isArray()) {
            if (!checkedType.isArray()) {
                return false;
            }
            Class<?> baseComponentClass = baseType.getComponentType().resolve();
            Class<?> checkedComponentClass = checkedType.getComponentType().resolve();
            if (isPrimitiveWrapRelation(baseComponentClass, checkedComponentClass)) {
                return false;
            }
            return compatibleOrNot(baseType.getComponentType(), checkedType.getComponentType());
        }

        // 类型字符串一样时还需要检查类加载器是否一样
        if (checkedType.toString().equals(baseType.toString())) {

            // 外层Class的类加载器不一样，必然不兼容
            if (!Objects.equals(checkedType.resolve().getClassLoader(), baseType.resolve().getClassLoader())) {
                return false;
            }

            // 外层Class类型兼容时还需检查泛型类型的兼容性
            ResolvableType[] checkedGenerics = checkedType.getGenerics();
            ResolvableType[] baseTypeGenerics = baseType.getGenerics();
            for (int i = 0; i < checkedGenerics.length; i++) {
                if (!compatibleOrNot(baseTypeGenerics[i], checkedGenerics[i])) {
                    return false;
                }
            }
            return true;
        }

        Class<?> baseClass = Objects.requireNonNull(baseType.resolve());
        Class<?> checkedClass = Objects.requireNonNull(checkedType.resolve());

        // 基本类型和其包装类型是相互兼容的
        if (isPrimitiveWrapRelation(baseClass, checkedClass)) {
            return true;
        }

        // 检查外部类型的兼容性，如果外部类型不兼容，则整个类型必然不兼容
        if (!baseClass.isAssignableFrom(checkedClass)) {
            return false;
        }

        /*--------------------外部类型兼容，检查泛型类型是否兼容--------------------*/

        // 基本类型没有泛型，则可以忽略泛型类型的比较
        if (!baseType.hasGenerics()) {
            return true;
        }

        // 基本类型带有泛型，则需要比较所有泛型类型的兼容性
        ResolvableType[] checkedTypeGenerics = checkedType.hasGenerics()
                ? checkedType.getGenerics()
                : ResolvableType.forClass(baseClass, checkedClass).getGenerics();
        ResolvableType[] baseTypeGenerics = baseType.getGenerics();

        for (int i = 0; i < baseTypeGenerics.length; i++) {
            if (!compatibleOrNot(baseTypeGenerics[i], checkedTypeGenerics[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 两个类型是否是原始类型与简单类型的关系
     *
     * @param class1 类型1
     * @param class2 类型1
     * @return 否是原始类型与简单类型的关系
     */
    public static boolean isPrimitiveWrapRelation(Class<?> class1, Class<?> class2) {
        if (!class1.isPrimitive() && !class2.isPrimitive()) {
            return false;
        }
        return ((class1 == int.class && class2 == Integer.class) || (class1 == Integer.class && class2 == int.class)) ||
                ((class1 == double.class && class2 == Double.class) || (class1 == Double.class && class2 == double.class)) ||
                ((class1 == char.class && class2 == Character.class) || (class1 == Character.class && class2 == char.class)) ||
                ((class1 == byte.class && class2 == Byte.class) || (class1 == Byte.class && class2 == byte.class)) ||
                ((class1 == short.class && class2 == Short.class) || (class1 == Short.class && class2 == short.class)) ||
                ((class1 == long.class && class2 == Long.class) || (class1 == Long.class && class2 == long.class)) ||
                ((class1 == float.class && class2 == Float.class) || (class1 == Float.class && class2 == float.class)) ||
                ((class1 == boolean.class && class2 == Boolean.class) || (class1 == Boolean.class && class2 == boolean.class));
    }

    /**
     * 获取一个类的继承结构
     *
     * @param aClass 类
     * @return 继承结构
     */
    public static Set<String> getInheritanceStructure(Class<?> aClass) {
        Set<String> proxyClassNameSet = new HashSet<>();
        if (aClass == null || aClass == Object.class) {
            return proxyClassNameSet;
        }
        proxyClassNameSet.add(aClass.getName());

        // 父类
        proxyClassNameSet.addAll(getInheritanceStructure(aClass.getSuperclass()));

        // 接口
        for (Class<?> anInterface : aClass.getInterfaces()) {
            proxyClassNameSet.addAll(getInheritanceStructure(anInterface));
        }

        return proxyClassNameSet;
    }

    /**
     * 获取对象的Class类型名称
     *
     * @param object 目标对象
     * @return 对象的Class类型名称
     */
    public static String getClassName(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }
}
