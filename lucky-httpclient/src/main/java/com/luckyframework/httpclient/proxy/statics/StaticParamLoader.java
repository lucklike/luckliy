package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;

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

    private final List<StaticParamAnalyzer> staticParamAnalyzers = new ArrayList<>();

    public StaticParamLoader(ObjectCreator objectCreator, Context context) {
        analyzerStaticParamAnnotation(objectCreator, context);
    }

    private void analyzerStaticParamAnnotation(ObjectCreator objectCreator, Context context) {
        Set<Annotation> staticParamAnnSet = context.getContainCombinationAnnotationsIgnoreSource(StaticParam.class);
        for (Annotation annotation : staticParamAnnSet) {
            StaticParam staticParamAnn = context.toAnnotation(annotation, StaticParam.class);

            // 获取参数设置器信息
            Class<? extends ParameterSetter> paramSetterClass = staticParamAnn.paramSetter();
            String paramSetterMsg = staticParamAnn.paramSetterMsg();

            // 获取参数解析器信息
            Class<? extends StaticParamResolver> paramResolverClass = staticParamAnn.paramResolver();
            String paramResolverMsg = staticParamAnn.paramResolverMsg();
            if (paramSetterClass == null || paramSetterMsg == null || paramResolverClass == null || paramResolverMsg == null) {
                continue;
            }

            // 构建参数设置器和参数解析器实例
            ParameterSetter parameterSetter = objectCreator.newObject(paramSetterClass, paramSetterMsg);
            StaticParamResolver staticParamResolver = objectCreator.newObject(paramResolverClass, paramResolverMsg);
            staticParamAnalyzers.add(new StaticParamAnalyzer(parameterSetter, staticParamResolver, annotation));
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
            List<ParamInfo> paramInfos = resolver.parser(new StaticParamAnnContext(context, staticParamAnnotation));
            for (ParamInfo paramInfo : paramInfos) {
                setter.set(request, paramInfo);
            }
        }
    }

}
