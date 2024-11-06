package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.spel.ContextSpELExecution;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.ProperSourcesParamWrapper;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.httpclient.proxy.spel.SpELVarManager;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT;

/**
 * 注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/31 16:00
 */
public class AnnotationContext implements SpELVarManager, ContextSpELExecution {


    /**
     * 上下文
     */
    private Context context;

    /**
     * 注解实例
     */
    private Annotation annotation;

    /**
     * 获取上下文
     *
     * @return 上下文
     */
    public Context getContext() {
        return context;
    }


    /**
     * 设置上下文
     *
     * @param context 上下文
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * 获取注解实例
     *
     * @return 重试注解实例
     */
    public Annotation getAnnotation() {
        return annotation;
    }

    /**
     * 设置注解实例
     *
     * @param annotation 注解实例
     */
    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    /**
     * 根据属性名获取注解中的属性值
     *
     * @param attributeName 属性名
     * @return 注解中对应的属性值
     */
    public Object getAnnotationAttribute(String attributeName) {
        return this.context.getAnnotationAttribute(this.annotation, attributeName);
    }

    /**
     * 根据属性名获取注解中的属性值，并转成具体的类型
     *
     * @param attributeName 属性名
     * @return 注解中对应的属性值
     */
    public <T> T getAnnotationAttribute(String attributeName, Class<T> type) {
        return this.context.getAnnotationAttribute(this.annotation, attributeName, type);
    }

    /**
     * 获取Http客户端代理对象工厂
     *
     * @return Http客户端代理对象工厂
     */
    public HttpClientProxyObjectFactory getHttpProxyFactory() {
        return this.context.getHttpProxyFactory();
    }

    /**
     * 获取当前正在执行的代理对象<br/>
     * 优先从本类中获取，本类中获取不到时会在父上下文中获取
     *
     * @return 当前正在执行的代理对象
     */
    public Object getProxyObject() {
        return context.getProxyObject();
    }

    /**
     * 从当前上下文中获取<b>合并注解</b>信息
     *
     * @param annotationClass 注解类型
     * @param <A>             注解类型
     * @return 注解实例
     */
    public <A extends Annotation> A getMergedAnnotation(Class<A> annotationClass) {
        return this.context.getMergedAnnotation(annotationClass);
    }

    /**
     * 从当前上下文中获取<b>合并注解</b>信息，本上下文中获取不到时会尝试从父上下文中获取
     *
     * @param annotationClass 注解类型
     * @param <A>             注解类型
     * @return 注解实例
     */
    public <A extends Annotation> A getMergedAnnotationCheckParent(Class<A> annotationClass) {
        return this.context.getMergedAnnotationCheckParent(annotationClass);
    }

    /**
     * 获取组合注解
     *
     * @param annotationClass 注解Class
     * @return 组合注解
     */
    public Annotation getCombinedAnnotation(Class<? extends Annotation> annotationClass) {
        return this.context.getCombinedAnnotation(annotationClass);
    }

    /**
     * 获取组合注解，本上下文中获取不到时会尝试从父上下文中获取
     *
     * @param annotationClass 注解Class
     * @return 组合注解
     */
    public Annotation getCombinedAnnotationCheckParent(Class<? extends Annotation> annotationClass) {
        return this.context.getCombinedAnnotationCheckParent(annotationClass);
    }

    /**
     * 获取同名的注解组合
     *
     * @param annotationClass 注解Class
     * @param <A>             注解类型
     * @return 同名的注解组合
     */
    public <A extends Annotation> A getSameAnnotationCombined(Class<A> annotationClass) {
        return this.context.getSameAnnotationCombined(annotationClass);
    }

    /**
     * 查找当前方法实例上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotationClass 注解类型
     * @param ignoreSourceAnn 是否忽略元注解类型的注解实例
     * @return 找到的所有组合注解实例
     */
    public <A extends Annotation> List<A> getNestCombinationAnnotations(Class<A> annotationClass, boolean ignoreSourceAnn) {
        return this.context.findNestCombinationAnnotations(annotationClass, ignoreSourceAnn);
    }

    /**
     * 【不忽略元注解类型的注解实例】
     * 查找当前方法实例上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotationClass 注解类型
     * @return 找到的所有组合注解实例
     */
    public <A extends Annotation> List<A> getNestCombinationAnnotations(Class<A> annotationClass) {
        return getNestCombinationAnnotations(annotationClass, false);
    }

    /**
     * 【不忽略元注解类型的注解实例】
     * 查找当前方法实例上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotationClass 注解类型
     * @return 找到的所有组合注解实例
     */
    public <A extends Annotation> List<A> getNestCombinationAnnotationsIgnoreSource(Class<A> annotationClass) {
        return getNestCombinationAnnotations(annotationClass, true);
    }

    /**
     * 检测当前注解元素是否被某个注解标注
     *
     * @param annotationClass 注解Class
     * @return true[被标注]/false[为被标注]
     */
    public boolean isAnnotated(Class<? extends Annotation> annotationClass) {
        return this.context.isAnnotated(annotationClass);
    }

    /**
     * 检查上下文链中的注解元素是否被某个注解标注
     *
     * @param annotationClass 注解Class
     * @return true[被标注]/false[为被标注]
     */
    public boolean isAnnotatedCheckParent(Class<? extends Annotation> annotationClass) {
        return this.context.isAnnotatedCheckParent(annotationClass);
    }

    /**
     * 是否存在注解实例
     *
     * @return true[存在]/false[不存在]
     */
    public boolean notNullAnnotated() {
        return this.annotation != null;
    }

    /**
     * 注解类型转换，将注解实例转化为某个具体类型
     *
     * @param annotationType 目标类型
     * @param <A>            目标类型泛型
     * @return 转化后的目标注解实例
     */
    public <A extends Annotation> A toAnnotation(Class<A> annotationType) {
        return context.toAnnotation(annotation, annotationType);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量，并转化为指定的类型
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getRootVar(String name, Class<T> typeClass) {
        return context.getRootVar(name, typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量，并转化为指定的类型，
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getNestRootVar(String name, Class<T> typeClass) {
        return context.getNestRootVar(name, typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getRootVar(String name) {
        return context.getRootVar(name);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getNestRootVar(String name) {
        return context.getNestRootVar(name);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量，并转化为指定的类型
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getVar(String name, Class<T> typeClass) {
        return context.getVar(name, typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量，并转化为指定的类型，
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getNestVar(String name, Class<T> typeClass) {
        return context.getNestVar(name, typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getVar(String name) {
        return context.getVar(name);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getNestVar(String name) {
        return context.getNestVar(name);
    }

    /**
     * 获取一个函数执行器
     *
     * @param name 函数名
     * @return 函数执行器
     */
    public FunExecutor getFun(String name) {
        return context.getFun(name);
    }

    /**
     * 函数调用
     *
     * @param name 函数名
     * @param args 参数列表
     * @param <T>  结果泛型
     * @return 运行结果
     */
    public <T> T callFun(String name, Object... args) {
        return context.callFun(name, args);
    }

    /**
     * 获取响应体转化元类型
     *
     * @return 转化元类型
     */
    public Class<?> getConvertMetaType() {
        return context.getConvertMetaType();
    }

    /**
     * 解析SpEL表达式，并将结果转化为指定的类型
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}  ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``  ->  表示不需要使用嵌套解析}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 结果类型
     * @param setter     参数设置器，用于向当前SpEL运行时环境中添加额外的参数
     * @param <T>        结果类型泛型
     * @return 表达式结果
     */
    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return context.parseExpression(expression, returnType, setter);
    }

    /**
     * [明确指定使用嵌套解析]
     * 解析SpEL表达式，并将结果转化为指定的类型
     *
     * @param expression SpEL表达式
     * @param returnType 结果类型
     * @param setter     参数设置器，用于向当前SpEL运行时环境中添加额外的参数
     * @param <T>        结果类型泛型
     * @return 表达式结果
     */
    @Override
    public <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return context.nestParseExpression(expression, returnType, setter);
    }

    /**
     * 对象实例生成
     *
     * @param objectGenerate 对象生成器注解实例
     * @param <T>            对象类型
     * @return 对象实例
     */
    public <T> T generateObject(ObjectGenerate objectGenerate) {
        return this.context.generateObject(objectGenerate);
    }

    /**
     * 对象实例生成
     *
     * @param clazz    对象Class
     * @param msg      创建对象的额外信息
     * @param scope    对象的作用域
     * @param consumer 对象的初始化方法
     * @param <T>      对象类型
     * @return 对象实例
     */
    public <T> T generateObject(Class<T> clazz, String msg, Scope scope, Consumer<T> consumer) {
        return this.context.generateObject(clazz, msg, scope, consumer);
    }

    /**
     * 对象实例生成
     *
     * @param clazz 对象Class
     * @param msg   创建对象的额外信息
     * @param scope 对象的作用域
     * @param <T>   对象类型
     * @return 对象实例
     */
    public <T> T generateObject(Class<T> clazz, String msg, Scope scope) {
        return this.context.generateObject(clazz, msg, scope);
    }

    /**
     * 对象实例生成，使用反射的方式来生成
     *
     * @param clazz 对象Class
     * @param scope 对象的作用域
     * @param <T>   对象类型
     * @return 对象实例
     */
    public <T> T generateObject(Class<T> clazz, Scope scope) {
        return this.context.generateObject(clazz, scope);
    }

    /**
     * 获取全局变量参数集
     *
     * @return 全局变量参数集
     */
    @NonNull
    @Override
    public MapRootParamWrapper getGlobalVar() {
        return context.getGlobalVar();
    }

    /**
     * 设置默认的上下文变量
     */
    @Override
    public void setContextVar() {
        this.context.setContextVar();
        context.getContextVar().addRootVariable(CONTEXT, LazyValue.of(this));
        context.getContextVar().addRootVariable(ANNOTATION_INSTANCE, LazyValue.of(this::getAnnotation));
    }

    /**
     * 获取当前上下文变量集
     *
     * @return 当前上下文变量集
     */
    @NonNull
    @Override
    public MapRootParamWrapper getContextVar() {
        return context.getContextVar();
    }

    /**
     * 设置请求上下文变量集
     *
     * @param request 请求对象
     */
    @Override
    public void setRequestVar(Request request) {
        this.context.setRequestVar(request);
    }

    /**
     * 获取请求上下文变量集
     *
     * @return 请求上下文变量集
     */
    @NonNull
    @Override
    public MapRootParamWrapper getRequestVar() {
        return context.getRequestVar();
    }

    /**
     * 设置响应上下文变量集
     *
     * @param response 响应对象
     * @param context  上下文对象
     */
    @Override
    public void setResponseVar(Response response, Context context) {
        context.setResponseVar(response);
    }

    /**
     * 设置响应上下文变量集
     *
     * @param response 响应对象
     */
    public void setResponseVar(Response response) {
        this.context.setResponseVar(response);
    }

    /**
     * 获取响应上下文变量集
     *
     * @return 响应上下文变量集
     */
    @NonNull
    @Override
    public MapRootParamWrapper getResponseVar() {
        return context.getResponseVar();
    }

    /**
     * 获取最终的SpEL运行时参数集
     *
     * @return 最终的SpEL运行时参数集
     */
    @NonNull
    @Override
    public ProperSourcesParamWrapper getFinallyVar() {
        return context.getFinallyVar();
    }

    /**
     * IF表达式计算
     *
     * @param expression 表达式
     * @return 计算结果
     */
    public String ifExpressionEvaluation(String expression) {
        return context.ifExpressionEvaluation(expression);
    }
}
