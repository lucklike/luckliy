package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.AbstractSpELResponseConvert;
import com.luckyframework.httpclient.proxy.convert.ConditionalSelectionException;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;


/**
 * 对环境变量API提供支持的类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 */
public class ConfigurationApiFunctionalSupport extends AbstractSpELResponseConvert implements StaticParamResolver, Interceptor {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationApiFunctionalSupport.class);

    private final static Map<String, ConfigurationSource> configSourceMap = new ConcurrentHashMap<>(4);

    static {
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
    private Api api;

    /**
     * 用于存储于接口相关的环变量的容器
     */
    private ConfigurationMap configMap;

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
        for (InterceptorConf conf : configApi.getInterceptor()) {
            Interceptor interceptor = createInterceptor(methodContext, conf);
            interceptor.beforeExecute(request, context);
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
        for (InterceptorConf conf : configApi.getInterceptor()) {
            Interceptor interceptor = createInterceptor(methodContext, conf);
            response = interceptor.afterExecute(response, context);
        }
        return response;
    }

    @SuppressWarnings("all")
    private ConfigApi createApi(StaticParamAnnContext context) {
        MethodContext methodContext = context.getContext();
        EnableConfigurationParser ann = context.toAnnotation(EnableConfigurationParser.class);

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
            configMap = configurationSource.getConfigMap(ann.source(), prefix);
            if (!configMap.containsConfigKey(prefix)) {
                throw new ConfigurationParserException("Configuration source no configuration information with the prefix '{}' is found in the '{}'.", prefix, ann.source());
            }
            api = configMap.getEntry(prefix, Api.class);
        }

        String methodName = methodContext.getCurrentAnnotatedElement().getName();
        String apiKey = keyProfix + methodName;
        return envApiMap.computeIfAbsent(methodName, k -> {
            ConfigApi configApi = configMap.getEntry(apiKey, ConfigApi.class);
            if (configApi == null) {
                throw new ConfigurationParserException("No configuration for the '{}' API is found in the source '{}': prefix = '{}'", methodName, ann.source(), prefix);
            }
            configApi.setApi(api);
            return configApi;
        });
    }

    private ConfigApi getConfigApi(MethodContext context) {
        String methodName = context.getCurrentAnnotatedElement().getName();
        return envApiMap.get(methodName);
    }

    /**
     * 使用拦截器配置创建一个拦截器实例
     *
     * @param context 当前方法上下文实例对象
     * @param conf    拦截器配置
     * @return 拦截器实例
     */
    private Interceptor createInterceptor(MethodContext context, InterceptorConf conf) {
        return (Interceptor) context.getHttpProxyFactory().getObjectCreator().newObject(conf.getClazz(), conf.getBeanName(), context, conf.getScope());
    }
}
