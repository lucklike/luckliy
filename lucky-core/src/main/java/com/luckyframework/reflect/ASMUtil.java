package com.luckyframework.reflect;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * asm工具
 * @author fk-7075
 *
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
	     * @param m
	     * @return
	     */
	    public static String[] getMethodParamNames(final Method m) {
	        final String[] paramNames = new String[m.getParameterTypes().length];
	        final Class<?> aClass = m.getDeclaringClass();
	        String aClassPath=aClass.getName().replaceAll("\\.","/")+".class";

			ClassReader cr = null;
	        try {
				InputStream in = aClass.getClassLoader().getResourceAsStream(aClassPath);
	            cr = new ClassReader(in);
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
	        cr.accept(new ClassVisitor(Opcodes.ASM6) {

	            @Override
	            public MethodVisitor visitMethod(final int access,
												 final String name, final String desc,
												 final String signature, final String[] exceptions) {
	                final Type[] args = Type.getArgumentTypes(desc);
	                // 方法名相同并且参数个数相同
	                if (!name.equals(m.getName())
	                        || !sameType(args, m.getParameterTypes())) {
	                    return super.visitMethod(access, name, desc, signature,
	                            exceptions);
	                }
	                MethodVisitor v = super.visitMethod(access, name, desc,
	                        signature, exceptions);
	                return new MethodVisitor(Opcodes.ASM6, v) {
	                    @Override
	                    public void visitLocalVariable(String name, String desc,
	                            String signature, Label start, Label end, int index) {
	                        int i = index - 1;
	                        // 如果是静态方法，则第一就是参数
	                        // 如果不是静态方法，则第一个是"this"，然后才是方法的参数
	                        if (Modifier.isStatic(m.getModifiers())) {
	                            i = index;
	                        }
	                        if (i >= 0 && i < paramNames.length) {
	                            paramNames[i] = name;
	                        }
	                        super.visitLocalVariable(name, desc, signature, start,
	                                end, index);
	                    }

	                };
	            }
	        }, 0);
	        return paramNames;
	    }

	/**
	 * 获取接口方法的参数名（抽象方法也可以）
	 * 编译时增加参数  -parameters
	 * @param method
	 * @return
	 * @throws IOException
	 */
	public static List<String> getInterfaceMethodParamNames(final Method method) throws IOException {
		final List<String> methodParametersNames = new ArrayList<>();
		final Class<?> aClass = method.getDeclaringClass();
		String aClassPath=aClass.getName().replaceAll("\\.","/")+".class";
		ClassReader cr = new ClassReader(Objects.requireNonNull(aClass.getClassLoader().getResourceAsStream(aClassPath)));
		ClassVisitor classVisitor=new ClassVisitor(Opcodes.ASM6) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

				final Type[] args = Type.getArgumentTypes(descriptor);

				// 方法名相同并且参数个数相同
				if (!name.equals(method.getName())|| !sameType(args, method.getParameterTypes())) {
					return super.visitMethod(access, name, descriptor, signature,exceptions);
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

		cr.accept(classVisitor,ClassReader.SKIP_FRAMES);

		return methodParametersNames;
	}

	public static List<String> getClassOrInterfaceMethodParamNames(final Method method) throws IOException {
		Class<?> declaringClass = method.getDeclaringClass();
		if(Modifier.isInterface(declaringClass.getModifiers())){
			return getInterfaceMethodParamNames(method);
		}
		return Arrays.asList(getMethodParamNames(method));
	}

	public static void main(String[] args) throws IOException {
		Method method = MethodUtils.getMethod(ASMUtil.class, "getClassOrInterfaceMethodParamNames", Method.class);
		System.out.println(getClassOrInterfaceMethodParamNames(method));

	}


}
