package com.luckyframework.reflect;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.exception.LuckyReflectionException;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * asm工具
 *
 * @author fk-7075
 */
public class ASMUtil {

    private static boolean sameType(Type[] types, Class<?>[] clazzes) {
        // 个数不同
        if (types.length != clazzes.length) {
            return false;
        }

        for (int i = 0; i < types.length; i++) {
            if (!Type.getType(clazzes[i]).equals(types[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取方法的参数名,无法获取接口参数名和JDK自带的类的方法参数名
     *
     * @param m 方法实例
     * @return 方法的参数名
     */
    public static String[] getMethodParamNames(final Method m) {
        final String[] paramNames = new String[m.getParameterTypes().length];
        final Class<?> aClass = m.getDeclaringClass();
        String aClassPath = aClass.getName().replaceAll("\\.", "/") + ".class";

        ClassReader cr = null;
        try {
            InputStream in = ClassUtils.getDefaultClassLoader().getResourceAsStream(aClassPath);
            cr = new ClassReader(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cr.accept(new ClassVisitor(Opcodes.ASM6) {

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                final Type[] args = Type.getArgumentTypes(desc);
                // 方法名相同并且参数个数相同
                if (!name.equals(m.getName()) || !sameType(args, m.getParameterTypes())) {
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
                MethodVisitor v = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM6, v) {
                    @Override
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        int i = index - 1;
                        // 如果是静态方法，则第一就是参数
                        // 如果不是静态方法，则第一个是"this"，然后才是方法的参数
                        if (Modifier.isStatic(m.getModifiers())) {
                            i = index;
                        }
                        if (i >= 0 && i < paramNames.length) {
                            paramNames[i] = name;
                        }
                        super.visitLocalVariable(name, desc, signature, start, end, index);
                    }

                };
            }
        }, 0);
        return paramNames;
    }

    /**
     * 获取接口方法的参数名（抽象方法也可以）
     * 编译时增加参数  -parameters
     *
     * @param method 方法实例
     * @return 方法参数列表名称
     * @throws IOException 可能存在IO异常
     */
    public static List<String> getInterfaceMethodParamNames(final Method method) throws IOException {
        final List<String> methodParametersNames = new ArrayList<>();
        final Class<?> aClass = method.getDeclaringClass();
        String aClassPath = aClass.getName().replaceAll("\\.", "/") + ".class";
        ClassReader cr = new ClassReader(Objects.requireNonNull(ClassUtils.getDefaultClassLoader().getResourceAsStream(aClassPath)));
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM6) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

                final Type[] args = Type.getArgumentTypes(descriptor);

                // 方法名相同并且参数个数相同
                if (!name.equals(method.getName()) || !sameType(args, method.getParameterTypes())) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }

                MethodVisitor v = super.visitMethod(access, name, descriptor, signature, exceptions);

                return new MethodVisitor(Opcodes.ASM6, v) {
                    /**
                     * 获取 MethodParameters 参数
                     */
                    @Override
                    public void visitParameter(String name, int access) {
                        methodParametersNames.add(name);
                        super.visitParameter(name, access);
                    }
                };
            }
        };

        cr.accept(classVisitor, ClassReader.SKIP_FRAMES);

        return methodParametersNames;
    }

    public static List<String> getClassOrInterfaceMethodParamNames(final Method method) throws IOException {
        Class<?> declaringClass = method.getDeclaringClass();
        if (Modifier.isInterface(declaringClass.getModifiers())) {
            return getInterfaceMethodParamNames(method);
        }
        return Arrays.asList(getMethodParamNames(method));
    }

    private static List<Field> getDeclaredFieldOrderList(Class<?> clazz) throws IOException {
        List<Field> fields = new ArrayList<>();

        // 指定要读取的类文件路径
        String className = clazz.getName(); // 以类的完全限定名（包名+类名）为例

        // 读取类文件
        InputStream in = ClassUtils.getDefaultClassLoader().getResourceAsStream(className.replace('.', '/') + ".class");

        // 使用 ASM 解析类文件
        ClassReader classReader = new ClassReader(in);
        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                try {
                    fields.add(clazz.getDeclaredField(name));
                } catch (NoSuchFieldException e) {
                    throw new LuckyReflectionException(e);
                }
                return super.visitField(access, name, descriptor, signature, value);
            }
        }, 0);

        return fields;
    }

    private static Field[] getDeclaredFieldOrder(Class<?> clazz) {
        try {
            return getDeclaredFieldOrderList(clazz).toArray(new Field[0]);
        } catch (Exception e) {
            throw new LuckyReflectionException(e);
        }
    }

    public static List<Field> getAllStaticFieldOrder(Class<?> clazz) {
        return Arrays.stream(getAllFieldsOrder(clazz))
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .collect(Collectors.toList());
    }

    /**
     * 获取一个类中所有属性实例，通过继承结构向上找，直到Object类结束
     *
     * @param clazz 目标类的Class
     * @return 类中所有属性实例
     */
    public static Field[] getAllFieldsOrder(Class<?> clazz) {
        // 传入的类为null时返回空数组
        if (clazz == null) {
            return new Field[0];
        }

        // 获取父类和所有接口类
        Class<?> superclass = clazz.getSuperclass();
        Class<?>[] interfaces = clazz.getInterfaces();

        // 父类为Object而且该类没有实现任何接口
        if (superclass == Object.class && ContainerUtils.isEmptyArray(interfaces)) {
            return getDeclaredFieldOrder(clazz);
        }

        // 获取本类的所有属性
        Field[] currentFields = getDeclaredFieldOrder(clazz);

        // 获取所有接口中的属性
        List<Field[]> supersFields = new ArrayList<>();
        for (Class<?> anInterface : interfaces) {
            supersFields.add(getAllFieldsOrder(anInterface));
        }
        // 获取父类中的所有属性
        supersFields.add(getAllFieldsOrder(superclass));

        // 合并所有属性
        return delCoverFields(currentFields, supersFields);
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

    private static List<Method> getDeclaredMethodOrderList(Class<?> clazz) throws IOException {

        List<Method> methods = new ArrayList<>();

        // 指定要读取的类文件路径
        String className = clazz.getName(); // 以类的完全限定名（包名+类名）为例

        // 读取类文件
        InputStream in = ClassUtils.getDefaultClassLoader().getResourceAsStream(className.replace('.', '/') + ".class");

        // 使用 ASM 解析类文件
        ClassReader classReader = new ClassReader(in);
        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!name.equals("<init>") && !name.equals("<clinit>")) {
                    Type[] argumentTypes = Type.getArgumentTypes(descriptor);
                    Class<?>[] argumentClasses = Stream.of(argumentTypes).map(t -> toClass(t, clazz.getClassLoader())).toArray(Class[]::new);
                    try {
                        methods.add(clazz.getDeclaredMethod(name, argumentClasses));
                    } catch (NoSuchMethodException e) {
                        throw new LuckyReflectionException(e);
                    }
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);

        return methods;
    }

    private static Method[] getDeclaredMethodOrder(Class<?> clazz) {
        try {
            return getDeclaredMethodOrderList(clazz).toArray(new Method[0]);
        } catch (Exception e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 保证顺序性
     * 获取一个类中所有静态方法实例，通过继承结构向上找，直到Object类结束
     *
     * @param clazz 目标类的Class
     * @return 类中所有方法实例
     */
    public static List<Method> getAllStaticMethodOrder(Class<?> clazz) {
        return Arrays.stream(getAllMethodOrder(clazz))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .collect(Collectors.toList());
    }

    /**
     * 保证顺序性
     * 获取一个类中所有方法实例，通过继承结构向上找，直到Object类结束
     *
     * @param clazz 目标类的Class
     * @return 类中所有方法实例
     */
    public static Method[] getAllMethodOrder(Class<?> clazz) {
        // 传入的类为null时返回空数组
        if (clazz == null) {
            return new Method[0];
        }

        // 获取父类和所有接口类
        Class<?> superclass = clazz.getSuperclass();
        Class<?>[] interfaces = clazz.getInterfaces();

        // 父类为Object而且该类没有实现任何接口
        if (superclass == Object.class && ContainerUtils.isEmptyArray(interfaces)) {
            return getDeclaredMethodOrder(clazz);
        }

        // 获取本类的所有方法
        Method[] currentMethods = getDeclaredMethodOrder(clazz);

        // 获取所有接口中的方法
        List<Method[]> supersMethods = new ArrayList<>();
        for (Class<?> anInterface : clazz.getInterfaces()) {
            supersMethods.add(getAllMethodOrder(anInterface));
        }

        // 获取父类中的所有方法
        supersMethods.add(getAllMethodOrder(clazz.getSuperclass()));

        // 合并所有方法
        return delCoverMethods(currentMethods, supersMethods);
    }

    /**
     * 过滤掉被@Cover注解标注的方法
     *
     * @param thisMethods   当前类的所有方法
     * @param supersMethods 当前类父类的所有方法
     * @return 过滤掉被@Cover注解标注的方法
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
     * 将 ASM Type 转换为 Java Class
     *
     * @param type ASM Type 对象
     * @return 对应的 Java Class 对象
     * @throws LuckyReflectionException 如果无法找到类，抛出此异常
     */
    public static Class<?> toClass(Type type, ClassLoader classLoader) {
        String descriptor = type.getDescriptor();

        // 如果是基本类型
        switch (descriptor) {
            case "Z":
                return boolean.class;
            case "B":
                return byte.class;
            case "C":
                return char.class;
            case "D":
                return double.class;
            case "F":
                return float.class;
            case "I":
                return int.class;
            case "J":
                return long.class;
            case "S":
                return short.class;
            case "V":
                return void.class;
        }

        // 如果是数组类型
        if (descriptor.startsWith("[")) {
            // 递归处理数组元素类型
            Class<?> componentClass = toClass(Type.getType(descriptor.substring(1)), classLoader);
            return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
        }

        // 处理对象类型
        if (descriptor.startsWith("L")) {
            // 去掉 'L' 和结尾的 ';'
            String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
            return ClassUtils.forName(className, classLoader);
        }

        // 无法解析的类型
        throw new LuckyReflectionException("Unknown descriptor: " + descriptor);
    }


}
