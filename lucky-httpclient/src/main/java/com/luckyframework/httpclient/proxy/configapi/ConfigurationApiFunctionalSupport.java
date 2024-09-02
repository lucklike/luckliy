package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.AbstractSpELResponseConvert;
import com.luckyframework.httpclient.proxy.convert.ConditionalSelectionException;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.handle.HttpExceptionHandle;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorContext;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformer;
import com.luckyframework.httpclient.proxy.interceptor.PrintLogInterceptor;
import com.luckyframework.httpclient.proxy.interceptor.RedirectInterceptor;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.sse.SseResponseConvert;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;
import com.luckyframework.loosebind.LooseBind;
import com.luckyframework.spel.LazyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQ_DEFAULT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQ_SSE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.configapi.Source.LOCAL_FILE;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;


/**
 * 对环境变量API提供支持的类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 */
public class ConfigurationApiFunctionalSupport implements ResponseConvert, StaticParamResolver, Interceptor, HttpExceptionHandle {

    /**
     * 配置源解析器
     */
    private final static Map<String, ConfigurationSource> configSourceMap = new ConcurrentHashMap<>(4);

    static {
        // 默认支持本地文件配置源解析器
        addConfigSource(LOCAL_FILE, new LocalFileConfigurationSource());
    }

    /**
     * 松散绑定
     */
    private final LooseBind looseBind = new LooseBind();

    /**
     * 初始化标识
     */
    private final AtomicBoolean init = new AtomicBoolean(false);

    /**
     * 环境变量API相关的缓存
     */
    private final Map<String, ConfigApi> envApiMap = new ConcurrentHashMap<>(16);

    /**
     * 所有接口方法公用的请求参数
     */
    private CommonApi commonApi;

    /**
     * 用于存储于接口相关的环变量的容器
     */
    private ConfigurationMap configMap;

    /**
     * 转换器Map
     */
    private final Map<String, ResponseConvert> responseConvertMap = new ConcurrentHashMap<>(4);

    {
        responseConvertMap.put(REQ_DEFAULT, new ConfigurationApiResponseConvert());
        responseConvertMap.put(REQ_SSE, new SseResponseConvert());
    }

    /**
     * 添加一个配置源
     *
     * @param sourceType   配置源类型
     * @param configSource 配置源
     */
    public static void addConfigSource(String sourceType, ConfigurationSource configSource) {
        configSourceMap.put(sourceType, configSource);
    }

    /**
     * 静态参数解析器相关的实现
     *
     * @param context 静态注解上下文信息
     * @return 解析得到的参数对象
     */
    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        return Collections.singletonList(new ParamInfo("envApi", new ConfigContextApi(createApi(context), context.getContext())));
    }

    /**
     * 响应转换相关的实现
     *
     * @param response 响应实体
     * @param context  转化器注解上下文
     * @param <T>      返回值泛型
     * @return 方法返回值类型相对应的实例
     * @throws Throwable 转换过程中可能会抛出的异常
     */
    @Override
    @SuppressWarnings("all")
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        ConfigApi configApi = getConfigApi(context.getContext());
        String type = configApi.getType();
        ResponseConvert convert = responseConvertMap.getOrDefault(type, responseConvertMap.get(REQ_DEFAULT));
        return convert.convert(response, context);
    }

    /**
     * 请求拦截相关的实现
     *
     * @param request 请求对象
     * @param context 拦截器上下文
     */
    @Override
    public void doBeforeExecute(Request request, InterceptorContext context) {
        MethodContext methodContext = context.getContext();
        ConfigApi configApi = getConfigApi(methodContext);
        Set<String> prohibitSet = configApi.getInterceptorProhibit();

        List<PriorityEntity<Interceptor>> chain = new ArrayList<>();

        // 自定义拦截器
        for (InterceptorConf conf : configApi.getInterceptor()) {
            addInterceptor(chain, prohibitSet, conf.getPriority(), createInterceptor(methodContext, conf));
        }

        // 日志拦截器
        LoggerConf logger = configApi.getLogger();
        if (logger.isEnable() != null) {
            addInterceptor(chain, prohibitSet, logger.getPriority(), getPrintLogInterceptor(context, logger));
        }

        chain.sort(Comparator.comparingInt(PriorityEntity::getPriority));
        for (PriorityEntity<Interceptor> entity : chain) {
            InterceptorPerformer.beforeExecute(request, context, entity.getEntity());
        }
    }

    /**
     * 响应拦截相关的实现
     *
     * @param response 响应对象
     * @param context  拦截器上下文
     */
    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        MethodContext methodContext = context.getContext();
        ConfigApi configApi = getConfigApi(methodContext);
        Set<String> prohibitSet = configApi.getInterceptorProhibit();

        List<PriorityEntity<Interceptor>> chain = new ArrayList<>();

        // 自定义拦截器
        for (InterceptorConf conf : configApi.getInterceptor()) {
            addInterceptor(chain, prohibitSet, conf.getPriority(), createInterceptor(methodContext, conf));
        }

        // 重定向拦截器
        RedirectConf redirect = configApi.getRedirect();
        if (redirect.isEnable() != null) {
            addInterceptor(chain, prohibitSet, redirect.getPriority(), getRedirectInterceptor(context, redirect));
        }

        // 日志拦截器
        LoggerConf logger = configApi.getLogger();
        if (logger.isEnable() != null) {
            addInterceptor(chain, prohibitSet, logger.getPriority(), getPrintLogInterceptor(context, logger));
        }

        chain.sort(Comparator.comparingInt(PriorityEntity::getPriority));
        for (PriorityEntity<Interceptor> priorityEntity : chain) {
            response = InterceptorPerformer.afterExecute(response, context, priorityEntity.getEntity());
        }
        return response;
    }

    /**
     * 创建一个配置API
     * <pre>
     *     1.配置源未初始化时先初始化配置源
     *          a.解析得到{@link EnableConfigurationParser#sourceType()}值，使用该值匹配一个配置源解析器{@link ConfigurationSource}
     *          b.使用配置源解析器构建一个配置对象，并将配置对象中的公共配置解析为{@link CommonApi}对象
     *     2.从配置对象中获取当前执行的方法对应的配置对象{@link ConfigApi}并返回
     * </pre>
     *
     * @param context
     * @return
     */
    @SuppressWarnings("all")
    private ConfigApi createApi(StaticParamAnnContext context) {
        MethodContext methodContext = context.getContext();
        EnableConfigurationParser ann = context.toAnnotation(EnableConfigurationParser.class);

        // 获取前缀配置，未配置时默认使用当前类的全类名作为前缀
        String prefix = StringUtils.hasText(ann.prefix()) ? ann.prefix() : methodContext.getClassContext().getCurrentAnnotatedElement().getName();
        String keyProfix = prefix + ".";

        if (init.compareAndSet(false, true)) {
            String sourceType = ann.sourceType();
            if (!StringUtils.hasText(sourceType)) {
                throw new ConfigurationParserException("@EnableConfigurationParser 'sourceType' attribute of the annotation cannot be empty.");
            }

            ConfigurationSource configurationSource = configSourceMap.get(sourceType);
            if (configurationSource == null) {
                throw new ConfigurationParserException("No configuration source parser of type '{}' could be found.", sourceType);
            }
            String source = context.parseExpression(ann.source());
            configMap = configurationSource.getConfigMap(source, prefix);
            if (!configMap.containsConfigKey(prefix)) {
                throw new ConfigurationParserException("Configuration source no configuration information with the prefix '{}' is found in the '{}'.", prefix, source);
            }
            commonApi = new CommonApi();
            looseBind(commonApi, configMap.getEntry(prefix, LinkedHashMap.class));
            commonApi.getSpringElImport().importSpELRuntime(methodContext.getParentContext());
        }

        String apiName = getApiName(methodContext);
        String apiKey = keyProfix + apiName;
        return envApiMap.computeIfAbsent(apiName, k -> {
            ConfigApi configApi = new ConfigApi();
            if (configMap.containsConfigKey(apiKey)) {
                looseBind(configApi, configMap.getEntry(apiKey, LinkedHashMap.class));
            } else if (!context.isAnnotated(HttpRequest.class)) {
                throw new ConfigurationParserException("No configuration for the '{}' API is found in the source '{}': prefix = '{}'", apiName, context.parseExpression(ann.source()), prefix);
            }
            configApi.setApi(commonApi);
            return configApi;
        });
    }

    private void looseBind(Object object, Map<String, Object> map) {
        try {
            looseBind.binding(object, map);
        } catch (Exception e) {
            throw new ConfigurationParserException(e);
        }
    }

    private String getApiName(MethodContext context) {
        Api apiAnn = context.getMergedAnnotation(Api.class);
        return apiAnn == null ? context.getCurrentAnnotatedElement().getName() : apiAnn.value();
    }

    /**
     * 从配置源中获取当前方法相关的配置
     *
     * @param context 当前方法上下文
     * @return 当前方法相关的配置
     */
    private ConfigApi getConfigApi(MethodContext context) {
        return envApiMap.get(getApiName(context));
    }

    /**
     * 使用拦截器配置创建一个拦截器实例
     *
     * @param context 当前方法上下文实例对象
     * @param conf    拦截器配置
     * @return 拦截器实例
     */
    private Interceptor createInterceptor(MethodContext context, InterceptorConf conf) {
        return (Interceptor) context.generateObject(conf.getClassName(), conf.getBeanName(), conf.getScope());
    }

    /**
     * 获取日志拦截器
     *
     * @param context 注解上下文
     * @param logger  日志配置
     * @return 日志拦截器
     */
    private PrintLogInterceptor getPrintLogInterceptor(InterceptorContext context, LoggerConf logger) {
        return context.generateObject(PrintLogInterceptor.class, "", Scope.METHOD_CONTEXT, interceptor -> {
            String _false = "#{false}";
            String _true = "#{true}";
            interceptor.setReqCondition(logger.isEnable() && logger.isEnableReqLog() ? _true : _false);
            interceptor.setRespCondition(logger.isEnable() && logger.isEnableRespLog() ? _true : _false);
            if (logger.isEnable() && StringUtils.hasText(logger.getReqLogCondition())) {
                interceptor.setReqCondition(logger.getReqLogCondition());
            }
            if (logger.isEnable() && StringUtils.hasText(logger.getRespLogCondition())) {
                interceptor.setRespCondition(logger.getRespLogCondition());
            }
            interceptor.setPrintAnnotationInfo(logger.isEnableAnnotationLog());
            interceptor.setPrintArgsInfo(logger.isEnableArgsLog());
            interceptor.setForcePrintBody(logger.isForcePrintBody());
            Set<String> allowPrintLogBodyMimeTypes = logger.getSetAllowMimeTypes();
            if (ContainerUtils.isNotEmptyCollection(allowPrintLogBodyMimeTypes)) {
                interceptor.setAllowPrintLogBodyMimeTypes(allowPrintLogBodyMimeTypes);
            }
            Set<String> addAllowPrintLogBodyMimeTypes = logger.getAddAllowMimeTypes();
            if (ContainerUtils.isNotEmptyCollection(addAllowPrintLogBodyMimeTypes)) {
                interceptor.addAllowPrintLogBodyMimeTypes(addAllowPrintLogBodyMimeTypes);
            }
            interceptor.setAllowPrintLogBodyMaxLength(logger.getBodyMaxLength());
        });
    }

    /**
     * 获取重定向拦截器
     *
     * @param context  注解上下文
     * @param redirect 重定向配置
     * @return 重定向拦截器
     */
    private RedirectInterceptor getRedirectInterceptor(InterceptorContext context, RedirectConf redirect) {
        return context.generateObject(RedirectInterceptor.class, "", Scope.METHOD, interceptor -> {
            if (ContainerUtils.isNotEmptyArray(redirect.getStatus())) {
                interceptor.setRedirectStatus(redirect.getStatus());
            }
            if (StringUtils.hasText(redirect.getCondition())) {
                interceptor.setRedirectCondition(redirect.getCondition());
            }
            if (StringUtils.hasText(redirect.getLocation())) {
                interceptor.setRedirectLocationExp(redirect.getLocation());
            }
            interceptor.setMaxRedirectCount(redirect.getMaxCount());
        });
    }

    /**
     * 向拦截器执行链中新增一个拦截器
     *
     * @param chain       拦截器执行链
     * @param prohibitSet 禁止列表
     * @param priority    优先级
     * @param interceptor 拦截器实例
     */
    private void addInterceptor(List<PriorityEntity<Interceptor>> chain, Set<String> prohibitSet, int priority, Interceptor interceptor) {
        if (!prohibitSet.contains(interceptor.uniqueIdentification())) {
            chain.add(PriorityEntity.of(priority, interceptor));
        }
    }

    @Override
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        return null;
    }

    /**
     * 条件响应转换器
     */
    class ConfigurationApiResponseConvert extends AbstractSpELResponseConvert {

        @Override
        @SuppressWarnings("all")
        public <T> T convert(Response response, ConvertContext context) throws Throwable {
            ConfigApi configApi = getConfigApi(context.getContext());
            Convert convert = configApi.getRespConvert();

            // 配置了禁止转换时，直接将响应体转为方法返回值类型
            if (Objects.equals(Boolean.TRUE, configApi.getConvertProhibit())) {
                return response.getEntity(context.getRealMethodReturnType());
            }

            // 扩展处理器不为null时优先使用处理器来转换响应数据
            Extension<ResponseConvertHandle> convertExtend = convert.getConvert();
            if (convertExtend != null) {
                ExtendHandleConfig<ResponseConvertHandle> handleConfig = convertExtend.getHandle();
                Object config = convertExtend.getConfig();
                Class<ResponseConvertHandle> handleClass = handleConfig.getClassName();
                handleClass = handleClass == null ? ResponseConvertHandle.class : handleClass;
                ResponseConvertHandle handle = context.generateObject(handleClass, handleConfig.getBeanName(), handleConfig.getScope());
                return (T) handle.handle(context.getContext(), response, ConversionUtils.looseBind(handle.getType(), config));
            }

            Class<?> metaType = convert.getMetaType();

            // 将响应体懒加载值替换为元类型的实例
            if (Object.class != metaType) {
                context.getResponseVar().addRootVariable(RESPONSE_BODY, LazyValue.of(() -> getResponseBody(response, metaType)));
            }

            // 条件判断，满足不同的条件时执行不同的逻辑
            for (Condition condition : convert.getCondition()) {
                boolean assertion = context.parseExpression(condition.getAssertion(), boolean.class);
                if (assertion) {

                    // 响应结果转换
                    String result = condition.getResult();
                    if (StringUtils.hasText(result)) {
                        return context.parseExpression(result, context.getRealMethodReturnType());
                    }

                    // 异常处理
                    String exception = condition.getException();
                    if (StringUtils.hasText(exception)) {
                        throwException(context, exception);
                    }
                    throw new ConditionalSelectionException("The 'result' and 'exception' in the conversion configuration cannot be null at the same time");
                }
            }


            // 所有条件均不满足时，执行默认的响应结果转换
            String result = convert.getResult();
            if (StringUtils.hasText(result)) {
                return context.parseExpression(result, context.getRealMethodReturnType());
            }

            // 所有条件均不满足时，执行默认的异常处理
            String exception = convert.getException();
            if (StringUtils.hasText(exception)) {
                throwException(context, exception);
            }

            // 未配置响应转化时直接将响应体转为方法返回值类型
            return getMethodResult(response, context.getContext());
        }
    }
}
