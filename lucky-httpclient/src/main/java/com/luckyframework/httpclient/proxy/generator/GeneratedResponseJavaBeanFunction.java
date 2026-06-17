package com.luckyframework.httpclient.proxy.generator;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

import java.io.UnsupportedEncodingException;

/**
 * 生成响应JavaBean对象需要用到的函数
 */
public class GeneratedResponseJavaBeanFunction {


    /**
     * 获取项目源代码的根路径
     *
     * @param clazz 目标Class
     * @return 项目源代码的根路径
     * @throws UnsupportedEncodingException 可能会出现的异常
     */
    @FunctionAlias("get_source_root_path")
    public static String getSourceRootPath(Class<?> clazz) throws UnsupportedEncodingException {
        return SourcePathUtils.getSourceRootPath(clazz);
    }

    /**
     * 获取某个Class的包名
     *
     * @param clazz 目标Class
     * @return 目标Class的包名
     */
    @FunctionAlias("get_def_package_name")
    public static String getDefPackageName(Class<?> clazz) {
        String name = clazz.getPackage().getName();
        return StringUtils.hasText(name) ? name + ".resp" : "resp";
    }

    /**
     * 获取默认的类名
     *
     * @param mc 方法上下文
     * @return 默认类名
     */
    @FunctionAlias("get_def_class_name")
    public static String getDefClassName(MethodContext mc) {
        return StringUtils.capitalize(mc.getCurrentAnnotatedElement().getName()) + "Response";
    }
}
