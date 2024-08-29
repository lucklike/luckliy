package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 静态参数加载器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 00:51
 */
public class StaticParamLoader {

    private final List<StaticParamAnalyzer> staticParamAnalyzers = new ArrayList<>();

    public StaticParamLoader(Context context) {
        analyzerStaticParamAnnotation(context);
    }

    private void analyzerStaticParamAnnotation(Context context) {
        Set<Annotation> staticParamAnnSet = context.getNestCombinationAnnotationsIgnoreSource(StaticParam.class);

        for (Annotation annotation : staticParamAnnSet) {
            // 获取静态参数注解和对象创建器
            StaticParam staticParamAnn = context.toAnnotation(annotation, StaticParam.class);

            staticParamAnalyzers.add(new StaticParamAnalyzer(
                    c -> c.generateObject(staticParamAnn.setter()),
                    c -> c.generateObject(staticParamAnn.resolver()),
                    annotation
            ));
        }
    }

    /**
     * 解析并且设置静态参数
     *
     * @param request 请求实例
     * @param context 环境上下文
     */
    public void resolverAndSetter(Request request, MethodContext context) {
        for (StaticParamAnalyzer staticParamAnalyzer : staticParamAnalyzers) {
            staticParamAnalyzer.resolverAndSetter(request, context);
        }
    }


    static
    class StaticParamAnalyzer {
        private final Function<MethodContext, ParameterSetter> setterFunction;
        private final Function<MethodContext, StaticParamResolver> resolverFunction;
        private final Annotation staticParamAnnotation;

        StaticParamAnalyzer(Function<MethodContext, ParameterSetter> setterFunction, Function<MethodContext, StaticParamResolver> resolverFunction, Annotation staticParamAnnotation) {
            this.setterFunction = setterFunction;
            this.resolverFunction = resolverFunction;
            this.staticParamAnnotation = staticParamAnnotation;
        }

        void resolverAndSetter(Request request, MethodContext context) {
            List<ParamInfo> paramInfos = resolverFunction.apply(context).parser(new StaticParamAnnContext(context, staticParamAnnotation));
            for (ParamInfo paramInfo : paramInfos) {
                setterFunction.apply(context).set(request, paramInfo);
            }
        }
    }

}
