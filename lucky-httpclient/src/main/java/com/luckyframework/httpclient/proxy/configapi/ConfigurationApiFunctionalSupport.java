package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.AbstractSpELResponseConvert;
import com.luckyframework.httpclient.proxy.convert.ConditionalSelectionException;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorContext;
import com.luckyframework.httpclient.proxy.interceptor.RedirectInterceptor;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.sse.SseResponseConvert;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;
import com.luckyframework.spel.LazyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQ_DEFAULT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQ_SSE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;


/**
 * 对环境变量API提供支持的类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 */
public class ConfigurationApiFunctionalSupport
        implements ResponseConvert, StaticParamResolver, Interceptor {

    /**
     * 配置源解析器
     */
    private final static Map<String, ConfigurationSource> configSourceMap = new ConcurrentHashMap<>(4);

    static {
        // 默认支持本地文件配置源解析器
        addConfigSource("file", new LocalFileConfigurationSource());
    }

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
        responseConvertMap.put(REQ_DEFAULT, new ConvertResponseConvert());
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
        List<PriorityEntity<Interceptor>> chain = new ArrayList<>();
        for (InterceptorConf conf : configApi.getInterceptor()) {
            chain.add(PriorityEntity.of(conf.getPriority(), createInterceptor(methodContext, conf)));
        }
        chain.sort(Comparator.comparingInt(PriorityEntity::getPriority));
        for (PriorityEntity<Interceptor> entity : chain) {
            entity.getEntity().doBeforeExecute(request, context);
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
        List<PriorityEntity<Interceptor>> chain = new ArrayList<>();
        for (InterceptorConf conf : configApi.getInterceptor()) {
            chain.add(PriorityEntity.of(conf.getPriority(), createInterceptor(methodContext, conf)));
        }
        RedirectConf redirect = configApi.getRedirect();
        if (redirect.isEnable()) {
            RedirectInterceptor redirectInterceptor = context.getHttpProxyFactory().getObjectCreator().newObject(RedirectInterceptor.class, "", context.getContext(), Scope.METHOD, interceptor -> {
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
            chain.add(PriorityEntity.of(redirect.getPriority(), redirectInterceptor));
        }

        chain.sort(Comparator.comparingInt(PriorityEntity::getPriority));
        for (PriorityEntity<Interceptor> priorityEntity : chain) {
            Interceptor interceptor = priorityEntity.getEntity();
            response = interceptor.afterExecute(response, context);
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
            commonApi = configMap.getEntry(prefix, CommonApi.class);
            commonApi.getSpringElImport().importSpELRuntime(methodContext.getParentContext());
        }

        String apiName = getApiName(methodContext);
        String apiKey = keyProfix + apiName;
        return envApiMap.computeIfAbsent(apiName, k -> {
            ConfigApi configApi = configMap.getEntry(apiKey, ConfigApi.class);
            if (configApi == null) {
                throw new ConfigurationParserException("No configuration for the '{}' API is found in the source '{}': prefix = '{}'", apiName, ann.source(), prefix);
            }
            configApi.setApi(commonApi);
            return configApi;
        });
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
        return (Interceptor) context.getHttpProxyFactory().getObjectCreator().newObject(conf.getClassName(), conf.getBeanName(), context, conf.getScope());
    }

    /**
     * 条件响应转换器
     */
    class ConvertResponseConvert extends AbstractSpELResponseConvert {

        @Override
        public <T> T convert(Response response, ConvertContext context) throws Throwable {
            ConfigApi configApi = getConfigApi(context.getContext());
            Convert convert = configApi.getRespConvert();
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
                        return context.parseExpression(
                                result,
                                context.getRealMethodReturnType()
                        );
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
                return context.parseExpression(
                        result,
                        context.getRealMethodReturnType()
                );
            }

            // 所有条件均不满足时，执行默认的异常处理
            String exception = convert.getException();
            if (StringUtils.hasText(exception)) {
                throwException(context, exception);
            }

            // 未配置响应转化时直接将响应体转为方法返回值类型
            return response.getEntity(context.getRealMethodReturnType());
        }
    }
}
