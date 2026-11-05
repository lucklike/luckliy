package com.luckyframework.httpclient.proxy.generator;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.hook.AsyncHook;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于从HTTP响应数据中自动生成Java实体类的注解。
 * 实体类将在响应生命周期阶段被生成。
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/6/15
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpELImport({GeneratedResponseJavaBeanFunction.class, GeneratedResponseJavaBean.CodeGeneratedCallback.class})
public @interface GeneratedResponseJavaBean {

    /**
     * SpEL表达式，用于启用/禁用代码生成。
     * 仅当此表达式计算结果为true时，才会执行代码生成。
     *
     * @return 启用条件表达式
     */
    String enable() default "true";

    /**
     * 允许生成实体类的方法返回值类型白名单。
     * <p>
     * 仅当代理方法的返回值类型（即 {@link MethodContext#getResultResolvableType()} 解析出的类型，
     * 使用结果处理器时取其泛型结果类型）是以下类型之一时，才会执行实体类生成；
     * 留空表示不做类型限制，所有返回值类型都会生成。
     * <p>
     * 匹配规则：只比较最外层类型，配置类型是返回值类型本身或其父类/接口即视为命中，
     * 例如配置 {@code Map.class} 可以命中返回值为 {@code HashMap} 的方法；
     * 不会递归检查泛型参数，{@code List<Xxx>} 只能被 {@code List.class} 或其父类型命中。
     * {@code Void.class} 与 {@code void} 视为同一类型。
     *
     * @return 返回值类型白名单
     */
    Class<?>[] generateTypes() default {};

    /**
     * SpEL表达式，用于判断是否覆盖已存在的文件。
     *
     * @return 覆盖条件表达式
     */
    String cover() default "false";

    /**
     * SpEL表达式，用于从响应中提取数据源以生成实体类。
     * 如果未指定，将使用整个响应体作为数据源。
     *
     * @return 提取表达式
     */
    String extractExpression() default "";

    /**
     * 必需。Java文件保存的目录路径。
     * 支持SpEL表达式。
     *
     * @return 保存路径
     */
    String savePath() default "#{get_source_root_path($class$)}";

    /**
     * 生成的Java类的包名。
     * 支持SpEL表达式。
     *
     * @return 包名
     */
    String packageName() default "#{get_def_package_name($class$)}";

    /**
     * 必需。主生成的Java类的类名。
     * 支持SpEL表达式。
     *
     * @return 类名
     */
    String className() default "#{get_def_class_name($mc$)}";

    /**
     * 是否使用Lombok注解（如 @Data、@Builder）。
     *
     * @return true表示使用Lombok，false表示不使用
     */
    boolean useLombok() default true;

    /**
     * 是否使用Swagger注解（如 @ApiModel、@ApiModelProperty）。
     *
     * @return true表示使用Swagger，false表示不使用
     */
    boolean useSwagger() default false;

    /**
     * 嵌套类的处理策略。
     *
     * @return 嵌套类策略
     */
    NestedClassStrategy nestedClassStrategy() default NestedClassStrategy.INNER_CLASS;

    /**
     * 从JSON字段名生成Java字段名的命名策略。
     *
     * @return 字段命名策略
     */
    FieldNamingStrategy fieldNamingStrategy() default FieldNamingStrategy.ORIGINAL;

    /**
     * 需要支持的序列化框架（通过注解方式）。
     *
     * @return 序列化框架
     */
    SerializationFramework serializationFramework() default SerializationFramework.NONE;

    /**
     * 生成的主类需要继承的父类。
     * 可写全限定名（如 com.demo.BaseResponse），也可写简单类名（仅限 java.lang 或同包可见）。
     * 支持SpEL表达式，表达式需返回类名字符串。
     * 留空表示不继承任何类。
     * 注意：只对主类生效，嵌套生成的类不会继承该类。
     *
     * @return 父类名
     */
    String extendClass() default "";

    /**
     * 生成的主类需要实现的接口，可配多个。
     * 每一项的写法与 {@link #extendClass()} 一致，支持SpEL表达式。
     * 注意：只对主类生效，且配置后需自行保证接口要求的方法能由生成的实体满足（如 Serializable 这类无业务方法的接口最安全）。
     *
     * @return 接口名数组
     */
    String[] implementInterfaces() default {};

    /**
     * 生成的主类是否实现 {@link java.io.Serializable}，并自动补上 serialVersionUID 字段。
     * 等价于在 {@link #implementInterfaces()} 里写全限定名，但无需关心包名与常量声明。
     *
     * @return true表示实现Serializable
     */
    boolean serializable() default false;

    /**
     * 落盘前是否用JDK自带的编译器对生成代码做一次内存编译自检，默认不开启。
     * 开启时，自检不通过的文件不会被写入源码目录，只在日志里回显错误；
     * 若运行环境是JRE（无编译器），自检会自动跳过。
     *
     * @return true表示执行自检
     */
    boolean validate() default false;

    /**
     * 是否额外生成一个把真实报文反序列化成生成实体的JUnit测试骨架，
     * 默认不生成：它需要测试目录与JUnit依赖，属于可选能力。
     * 生成位置由 {@link #testSavePath()} 决定，与实体同包，可直接引用实体类。
     *
     * @return true表示生成测试骨架
     */
    boolean generateExampleTest() default false;

    /**
     * 测试骨架的保存目录，仅在 {@link #generateExampleTest()} 为true时使用。
     * 支持SpEL表达式，默认定位到当前项目的 src/test/java。
     *
     * @return 测试目录路径
     */
    String testSavePath() default "#{get_test_source_root_path($class$)}";

    /**
     * 处理Java代码生成逻辑的回调类。
     */
    class CodeGeneratedCallback {

        /**
         * 是否启用Java代码生成的回调
         *
         * @param mc 方法上下文
         * @return 是否启用代码生成的回调
         */
        @FunctionAlias("generater_java_code_callback_enable")
        public static boolean generatedJavaCodeCallbackEnable(MethodContext mc) {
            GeneratedResponseJavaBean ann = mc.getSameAnnotationCombined(GeneratedResponseJavaBean.class);
            return StringUtils.hasText(ann.enable()) && mc.parseExpression(ann.enable(), boolean.class);
        }

        /**
         * 异步钩子方法，在响应生命周期阶段触发生成Java代码。
         * 此方法由框架自动调用，负责将注解配置转换为内部配置对象，
         * 并委托给 {@link GeneratedJavaCodeUtils} 执行实际的代码生成逻辑。
         *
         * @param mc       方法上下文
         * @param response HTTP响应对象
         * @throws IOException 文件写入失败时抛出
         */
        @AsyncHook
        @Callback(order = 100, enable = "#{generater_java_code_callback_enable($mc$)}", lifecycle = Lifecycle.RESPONSE, errorInterrupt = false)
        public static void generatedJavaCode(MethodContext mc, Response response) throws IOException {
            GeneratedResponseJavaBean ann = mc.getSameAnnotationCombined(GeneratedResponseJavaBean.class);
            GeneratedJavaCodeConfiguration codeConfiguration = convertToConfig(mc, ann);
            GeneratedJavaCodeUtils.generatedJavaCode(mc, response, codeConfiguration);
        }

        /**
         * 将 {@link GeneratedResponseJavaBean} 注解实例转换为 {@link GeneratedJavaCodeConfiguration} 配置对象
         *
         * @param mc  方法上下文
         * @param ann 注解实例
         * @return 配置对象
         */
        private static GeneratedJavaCodeConfiguration convertToConfig(MethodContext mc, GeneratedResponseJavaBean ann) {
            GeneratedJavaCodeConfiguration codeConfiguration = new GeneratedJavaCodeConfiguration();
            codeConfiguration.setEnable(StringUtils.hasText(ann.enable()) && mc.parseExpression(ann.enable(), boolean.class));
            codeConfiguration.setGenerateTypes(ann.generateTypes());
            codeConfiguration.setCover(StringUtils.hasText(ann.cover()) && mc.parseExpression(ann.cover(), boolean.class));
            codeConfiguration.setExtractExpression(ann.extractExpression());
            codeConfiguration.setSavePath(ann.savePath());
            codeConfiguration.setPackageName(ann.packageName());
            codeConfiguration.setClassName(ann.className());
            codeConfiguration.setUseLombok(ann.useLombok());
            codeConfiguration.setUseSwagger(ann.useSwagger());
            codeConfiguration.setNestedClassStrategy(ann.nestedClassStrategy());
            codeConfiguration.setFieldNamingStrategy(ann.fieldNamingStrategy());
            codeConfiguration.setSerializationFramework(ann.serializationFramework());
            codeConfiguration.setExtendClass(ann.extendClass());
            codeConfiguration.setImplementInterfaces(ann.implementInterfaces());
            codeConfiguration.setSerializable(ann.serializable());
            codeConfiguration.setValidate(ann.validate());
            codeConfiguration.setGenerateExampleTest(ann.generateExampleTest());
            codeConfiguration.setTestSavePath(ann.testSavePath());
            return codeConfiguration;
        }

    }

}