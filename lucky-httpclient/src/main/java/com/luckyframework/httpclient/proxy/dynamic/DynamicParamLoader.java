package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.paraminfo.CarrySetterParamInfo;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.setter.QueryParameterSetter;
import com.luckyframework.httpclient.proxy.setter.StandardBodyParameterSetter;
import com.luckyframework.httpclient.proxy.setter.StandardHttpFileParameterSetter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 动态参数加载器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 02:03
 */
@SuppressWarnings("unchecked")
public class DynamicParamLoader {

    public static final DynamicParamResolver STANDARD_HTTP_FILE_RESOLVER = new StandardHttpFileDynamicParamResolver();
    public static final DynamicParamResolver LOOK_UP_SPECIAL_ANNOTATION_RESOLVER = new LookUpSpecialAnnotationDynamicParamResolver();

    public static final ParameterSetter QUERY_SETTER = new QueryParameterSetter();
    public static final ParameterSetter STANDARD_HTTP_FILE_SETTER = new StandardHttpFileParameterSetter();
    public static final ParameterSetter STANDARD_BODY_SETTER = new StandardBodyParameterSetter();


    public static final Supplier<DynamicParamResolver> STANDARD_HTTP_FILE_RESOLVER_SUPPLIER = () -> STANDARD_HTTP_FILE_RESOLVER;
    public static final Supplier<DynamicParamResolver> LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_SUPPLIER = () -> LOOK_UP_SPECIAL_ANNOTATION_RESOLVER;

    public static final Supplier<ParameterSetter> QUERY_SETTER_SUPPLIER = () -> QUERY_SETTER;
    public static final Supplier<ParameterSetter> STANDARD_HTTP_FILE_SETTER_SUPPLIER = () -> STANDARD_HTTP_FILE_SETTER;
    public static final Supplier<ParameterSetter> STANDARD_BODY_SETTER_SUPPLIER = () -> STANDARD_BODY_SETTER;


    private final List<DynamicParamAnalyzer> dynamicParamAnalyzers = new ArrayList<>();

    public DynamicParamLoader(MethodContext methodContext) {
        analyzerDynamicParamAnnotation(methodContext);
    }

    private void analyzerDynamicParamAnnotation(MethodContext methodContext) {
        for (ParameterContext parameterContext : methodContext.getParameterContexts()) {
            if (parameterContext.isAnnotatedCheckParent(NotHttpParam.class)) {
                continue;
            }
            int index = parameterContext.getIndex();
            DynamicParam dynamicParamAnn = parameterContext.getSameAnnotationCombined(DynamicParam.class);

            // 当存在@DynamicParam注解时，使用注解中配置的ParameterSetter和DynamicParamResolver
            if (dynamicParamAnn != null) {
                TempPair<Supplier<ParameterSetter>, Supplier<DynamicParamResolver>> pair = defaultSetterResolver(dynamicParamAnn, QUERY_SETTER_SUPPLIER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_SUPPLIER, methodContext);
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, pair.getOne(), pair.getTwo(), dynamicParamAnn));
            }
            // 忽略空值和响应处理器
            else if (parameterContext.isNullValue() || parameterContext.isResponseProcessorInstance()) {
                // ignore value
            }
            // 资源类型参数
            else if (parameterContext.isResourceType()) {
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, STANDARD_HTTP_FILE_SETTER_SUPPLIER, STANDARD_HTTP_FILE_RESOLVER_SUPPLIER));
            }
            // 请求体类型参数
            else if (parameterContext.isBodyObjectInstance()) {
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, STANDARD_BODY_SETTER_SUPPLIER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_SUPPLIER));
            }
            // 基本类型参数
            else if (parameterContext.isSimpleBaseType()) {
                TempPair<Supplier<ParameterSetter>, Supplier<DynamicParamResolver>> pair = defaultSetterResolver(null, QUERY_SETTER_SUPPLIER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_SUPPLIER, methodContext);
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, pair.getOne(), pair.getTwo()));
            }
            // 复杂类型参数
            else {
                TempPair<Supplier<ParameterSetter>, Supplier<DynamicParamResolver>> pair = defaultSetterResolver(null, QUERY_SETTER_SUPPLIER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_SUPPLIER, methodContext);
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(
                        index,
                        pair.getOne(),
                        StandardObjectDynamicParamResolver::new,
                        null
                ));
            }
        }
    }

    public static TempPair<Supplier<ParameterSetter>, Supplier<DynamicParamResolver>> defaultSetterResolver(DynamicParam dynamicParamAnn,
                                                                                                            Supplier<ParameterSetter> defaultSetterSupplier,
                                                                                                            Supplier<DynamicParamResolver> defaultResolverSupplier,
                                                                                                            Context context) {
        if (dynamicParamAnn == null) {
            return TempPair.of(defaultSetterSupplier, defaultResolverSupplier);
        }

        ObjectCreator objectCreator = HttpClientProxyObjectFactory.getObjectCreator();

        // 构建参数设置器和参数解析器
        Class<? extends DynamicParamResolver> resolverClass = (Class<? extends DynamicParamResolver>) dynamicParamAnn.resolver().clazz();
        Supplier<DynamicParamResolver> resolverSupplier =
                resolverClass == StandardObjectDynamicParamResolver.class
                        ? StandardObjectDynamicParamResolver::new
                        : () -> (DynamicParamResolver) objectCreator.newObject(dynamicParamAnn.resolver(), context);

        return TempPair.of(
                () -> (ParameterSetter) objectCreator.newObject(dynamicParamAnn.setter(), context),
                resolverSupplier
        );
    }

    public void resolverAndSetter(Request request, MethodContext methodContext) {
        for (DynamicParamAnalyzer dynamicParamAnalyzer : dynamicParamAnalyzers) {
            dynamicParamAnalyzer.resolverAndSetter(request, methodContext);
        }
    }


    static
    class DynamicParamAnalyzer {

        private final int index;
        private final Supplier<ParameterSetter> setterSupplier;
        private final Supplier<DynamicParamResolver> resolverSupplier;
        private final Annotation dynamicParamAnnotation;


        DynamicParamAnalyzer(int index, Supplier<ParameterSetter> setterSupplier, Supplier<DynamicParamResolver> resolverSupplier, Annotation dynamicParamAnnotation) {
            this.index = index;
            this.setterSupplier = setterSupplier;
            this.resolverSupplier = resolverSupplier;
            this.dynamicParamAnnotation = dynamicParamAnnotation;
        }

        public DynamicParamAnalyzer(int index, Supplier<ParameterSetter> setterSupplier, Supplier<DynamicParamResolver> resolverSupplier) {
            this(index, setterSupplier, resolverSupplier, null);
        }

        private void resolverAndSetter(Request request, MethodContext methodContext) {
            ParameterContext parameterContext = methodContext.getParameterContexts().get(index);
            parameterContext.setParentContext(methodContext);
            List<? extends ParamInfo> paramInfos = resolverSupplier.get().parser(new DynamicParamContext(parameterContext, dynamicParamAnnotation));
            for (ParamInfo paramInfo : paramInfos) {
                if (paramInfo instanceof CarrySetterParamInfo) {
                    ((CarrySetterParamInfo) paramInfo).setParameter(request);
                } else {
                    setterSupplier.get().set(request, paramInfo);
                }
            }
        }
    }
}
