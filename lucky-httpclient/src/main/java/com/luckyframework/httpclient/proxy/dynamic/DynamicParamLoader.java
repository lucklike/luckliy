package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.CarrySetterParamInfo;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.setter.QueryParameterSetter;
import com.luckyframework.httpclient.proxy.setter.StandardBodyParameterSetter;
import com.luckyframework.httpclient.proxy.setter.StandardHttpFileParameterSetter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

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

    private final ObjectCreator objectCreator;
    private final List<DynamicParamAnalyzer> dynamicParamAnalyzers = new ArrayList<>();

    public DynamicParamLoader(ObjectCreator objectCreator, MethodContext methodContext) {
        this.objectCreator = objectCreator;
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
                TempPair<ParameterSetter, DynamicParamResolver> pair = defaultSetterResolver(objectCreator, dynamicParamAnn, QUERY_SETTER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER);
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, pair.getOne(), pair.getTwo(), dynamicParamAnn));
            }
            // 忽略空值和响应处理器
            else if (parameterContext.isNullValue() || parameterContext.isResponseProcessorInstance()) {
                // ignore value
            }
            // 资源类型参数
            else if (parameterContext.isResourceType()) {
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, STANDARD_HTTP_FILE_SETTER, STANDARD_HTTP_FILE_RESOLVER));
            }
            // 请求体类型参数
            else if (parameterContext.isBodyObjectInstance()) {
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, STANDARD_BODY_SETTER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER));
            }
            // 基本类型参数
            else if (parameterContext.isSimpleBaseType()) {
                TempPair<ParameterSetter, DynamicParamResolver> pair = defaultSetterResolver(objectCreator, null, QUERY_SETTER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER);
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, pair.getOne(), pair.getTwo()));
            }
            // 复杂类型参数
            else {
                TempPair<ParameterSetter, DynamicParamResolver> pair = defaultSetterResolver(objectCreator, null, QUERY_SETTER, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER);
                dynamicParamAnalyzers.add(new DynamicParamAnalyzer(
                        index,
                        pair.getOne(),
                        new StandardObjectDynamicParamResolver(),
                        null
                ));
            }
        }
    }

    public static TempPair<ParameterSetter, DynamicParamResolver> defaultSetterResolver(ObjectCreator objectCreator,
                                                                                        DynamicParam dynamicParamAnn,
                                                                                        ParameterSetter defaultSetter,
                                                                                        DynamicParamResolver defaultResolver) {
        if (dynamicParamAnn == null) {
            return TempPair.of(defaultSetter, defaultResolver);
        }
        // 从@DynamicParam注解中获取DynamicParamResolver和ParameterSetter的创建信息
        Class<? extends ParameterSetter> paramSetterClass = dynamicParamAnn.paramSetter();
        String paramSetterMsg = dynamicParamAnn.paramSetterMsg();

        Class<? extends DynamicParamResolver> paramResolverClass = dynamicParamAnn.paramResolver();
        String paramResolverMsg = dynamicParamAnn.paramResolverMsg();

        ParameterSetter parameterSetter = objectCreator.newObject(paramSetterClass, paramSetterMsg);
        DynamicParamResolver paramResolver = paramResolverClass == StandardObjectDynamicParamResolver.class
                ? new StandardObjectDynamicParamResolver()
                : objectCreator.newObject(paramResolverClass, paramResolverMsg);
        return TempPair.of(parameterSetter, paramResolver);
    }

    public void resolverAndSetter(Request request, MethodContext methodContext) {
        for (DynamicParamAnalyzer dynamicParamAnalyzer : dynamicParamAnalyzers) {
            dynamicParamAnalyzer.resolverAndSetter(request, methodContext);
        }
    }


    static
    class DynamicParamAnalyzer {

        private final int index;
        private final ParameterSetter setter;
        private final DynamicParamResolver resolver;
        private final Annotation dynamicParamAnnotation;


        DynamicParamAnalyzer(int index, ParameterSetter setter, DynamicParamResolver resolver, Annotation dynamicParamAnnotation) {
            this.index = index;
            this.setter = setter;
            this.resolver = resolver;
            this.dynamicParamAnnotation = dynamicParamAnnotation;
        }

        public DynamicParamAnalyzer(int index, ParameterSetter setter, DynamicParamResolver resolver) {
            this(index, setter, resolver, null);
        }

        private void resolverAndSetter(Request request, MethodContext methodContext) {
            ParameterContext parameterContext = methodContext.getParameterContexts().get(index);
            parameterContext.setParentContext(methodContext);
            List<? extends ParamInfo> paramInfos = resolver.parser(new DynamicParamContext(parameterContext, dynamicParamAnnotation));
            for (ParamInfo paramInfo : paramInfos) {
                if (paramInfo instanceof CarrySetterParamInfo) {
                    ((CarrySetterParamInfo) paramInfo).setParameter(request);
                } else {
                    setter.set(request, paramInfo);
                }
            }
        }
    }
}
