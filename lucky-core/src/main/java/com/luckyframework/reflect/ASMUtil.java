package com.luckyframework.reflect;

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
            InputStream in = aClass.getClassLoader().getResourceAsStream(aClassPath);
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
        ClassReader cr = new ClassReader(Objects.requireNonNull(aClass.getClassLoader().getResourceAsStream(aClassPath)));
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
        }catch (Exception e) {
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
        if (clazz == null) {
            return new Field[0];
        }
        if (clazz.getSuperclass() == Object.class) {
            return clazz.getDeclaredFields();
        }
        Field[] currentFields = getDeclaredFieldOrder(clazz);

        List<Field[]> supersFields = new ArrayList<>();
        for (Class<?> anInterface : clazz.getInterfaces()) {
            supersFields.add(getAllFieldsOrder(anInterface));
        }
        supersFields.add(getAllFieldsOrder(clazz.getSuperclass()));
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
                    Class<?>[] argumentClasses = Stream.of(argumentTypes).map(t -> ClassUtils.forName(t.getClassName(), clazz.getClassLoader())).toArray(Class[]::new);
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
        }catch (Exception e) {
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
        if (clazz == null) {
            return new Method[0];
        }
        if (clazz.getSuperclass() == Object.class) {
            return clazz.getDeclaredMethods();
        }
        Method[] currentMethods = getDeclaredMethodOrder(clazz);

        List<Method[]> supersMethods = new ArrayList<>();
        for (Class<?> anInterface : clazz.getInterfaces()) {
            supersMethods.add(getAllMethodOrder(anInterface));
        }
        supersMethods.add(getAllMethodOrder(clazz.getSuperclass()));
        return delCoverMethods(currentMethods, supersMethods);
    }

    /**
     * 过滤掉被@Cover注解标注的方法
     *
     * @param thisMethods   当前类的所有方法
     * @param supersMethods 当前类父类的所有方法
     * @return  过滤掉被@Cover注解标注的方法
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

}
