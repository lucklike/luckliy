package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 静态参数加载器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 00:51
 */
public class StaticParamLoader {

    private static final String ANNOTATION_ATTRIBUTE_PARAM_SETTER = "paramSetter";
    private static final String ANNOTATION_ATTRIBUTE_PARAM_SETTER_MSG = "paramSetterMsg";
    private static final String ANNOTATION_ATTRIBUTE_PARAM_RESOLVER = "paramResolver";
    private static final String ANNOTATION_ATTRIBUTE_PARAM_RESOLVER_MSG = "paramResolverMsg";

    private final List<StaticParamAnalyzer> staticParamAnalyzers = new ArrayList<>();

    public StaticParamLoader(ObjectCreator objectCreator, Context context) {
        analyzerStaticParamAnnotation(objectCreator, context);
    }

    @SuppressWarnings("unchecked")
    private void analyzerStaticParamAnnotation(ObjectCreator objectCreator, Context context) {
        Set<Annotation> staticParamAnnSet = context.getContainCombinationAnnotationsIgnoreSource(StaticParam.class);
        for (Annotation staticParamAnn : staticParamAnnSet) {
            Class<? extends ParameterSetter> paramSetterClass = (Class<? extends ParameterSetter>) context.getAnnotationAttribute(staticParamAnn, ANNOTATION_ATTRIBUTE_PARAM_SETTER);
            String paramSetterMsg = context.getAnnotationAttribute(staticParamAnn, ANNOTATION_ATTRIBUTE_PARAM_SETTER_MSG, String.class);
            Class<? extends StaticParamResolver> paramResolverClass = (Class<? extends StaticParamResolver>) context.getAnnotationAttribute(staticParamAnn, ANNOTATION_ATTRIBUTE_PARAM_RESOLVER);
            String paramResolverMsg = context.getAnnotationAttribute(staticParamAnn, ANNOTATION_ATTRIBUTE_PARAM_RESOLVER_MSG, String.class);
            if (paramSetterClass == null || paramSetterMsg == null || paramResolverClass == null || paramResolverMsg == null) {
                continue;
            }

            ParameterSetter parameterSetter = objectCreator.newObject(paramSetterClass, paramSetterMsg);
            StaticParamResolver staticParamResolver = objectCreator.newObject(paramResolverClass, paramResolverMsg);
            staticParamAnalyzers.add(new StaticParamAnalyzer(parameterSetter, staticParamResolver, staticParamAnn));
        }
    }

    /**
     * 解析并且设置静态参数
     * @param request 请求实例
     * @param context 环境上下文
     */
    public void resolverAndSetter(Request request, MethodContext context) {
        for (StaticParamAnalyzer staticParamAnalyzer : staticParamAnalyzers) {
            staticParamAnalyzer.resolverAndSetter(request, context);
        }
    }


    class StaticParamAnalyzer {
        private final ParameterSetter setter;
        private final StaticParamResolver resolver;
        private final Annotation staticParamAnnotation;

        StaticParamAnalyzer(ParameterSetter setter, StaticParamResolver resolver, Annotation staticParamAnnotation) {
            this.setter = setter;
            this.resolver = resolver;
            this.staticParamAnnotation = staticParamAnnotation;
        }

        void resolverAndSetter(Request request, MethodContext context) {
            List<ParamInfo> paramInfos = resolver.parser(context, staticParamAnnotation);
            for (ParamInfo paramInfo : paramInfos) {
                setter.set(request, paramInfo);
            }
        }
    }

}
