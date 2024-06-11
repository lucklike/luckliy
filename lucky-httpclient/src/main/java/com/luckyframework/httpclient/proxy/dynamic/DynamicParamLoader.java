package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.paraminfo.CarrySetterParamInfo;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.luckyframework.httpclient.proxy.dynamic.DynamicParamConstant.LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_FUNCTION;
import static com.luckyframework.httpclient.proxy.dynamic.DynamicParamConstant.QUERY_SETTER_FUNCTION;

/**
 * 动态参数加载器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 02:03
 */
public class DynamicParamLoader {


    private final List<DynamicParamAnalyzer> dynamicParamAnalyzers = new ArrayList<>();

    public DynamicParamLoader(MethodContext methodContext) {
        analyzerDynamicParamAnnotation(methodContext);
    }

    private void analyzerDynamicParamAnnotation(MethodContext methodContext) {
        for (ParameterContext parameterContext : methodContext.getParameterContexts()) {
            if (parameterContext.notHttpParam()) {
                continue;
            }
            int index = parameterContext.getIndex();
            DynamicParam dynamicParamAnn = parameterContext.getSameAnnotationCombined(DynamicParam.class);
            TempPair<Function<Context, ParameterSetter>, Function<Context, DynamicParamResolver>>
                    pair = defaultSetterResolver(dynamicParamAnn, QUERY_SETTER_FUNCTION, LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_FUNCTION);
            dynamicParamAnalyzers.add(new DynamicParamAnalyzer(index, pair.getOne(), pair.getTwo(), dynamicParamAnn));
        }
    }

    public static TempPair<Function<Context, ParameterSetter>, Function<Context, DynamicParamResolver>> defaultSetterResolver(DynamicParam dynamicParamAnn,
                                                                                                                                                  Function<Context, ParameterSetter> defaultSetterFunction,
                                                                                                                                                  Function<Context, DynamicParamResolver> defaultResolverFunction) {
        if (dynamicParamAnn == null) {
            return TempPair.of(defaultSetterFunction, defaultResolverFunction);
        }

        // 构建参数设置器和参数解析器
        return TempPair.of(
                c -> c.generateObject(dynamicParamAnn.setter()),
                c -> c.generateObject(dynamicParamAnn.resolver())
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
        private final Function<Context, ParameterSetter> setterFunction;
        private final Function<Context, DynamicParamResolver> resolverFunction;
        private final Annotation dynamicParamAnnotation;


        DynamicParamAnalyzer(int index, Function<Context, ParameterSetter> setterFunction, Function<Context, DynamicParamResolver> resolverFunction, Annotation dynamicParamAnnotation) {
            this.index = index;
            this.setterFunction = setterFunction;
            this.resolverFunction = resolverFunction;
            this.dynamicParamAnnotation = dynamicParamAnnotation;
        }

        public DynamicParamAnalyzer(int index, Function<Context, ParameterSetter> setterFunction, Function<Context, DynamicParamResolver> resolverFunction) {
            this(index, setterFunction, resolverFunction, null);
        }

        private void resolverAndSetter(Request request, MethodContext methodContext) {
            ParameterContext parameterContext = methodContext.getParameterContexts()[index];
            parameterContext.setParentContext(methodContext);
            List<? extends ParamInfo> paramInfos = resolverFunction.apply(methodContext).parser(new DynamicParamContext(parameterContext, dynamicParamAnnotation));
            for (ParamInfo paramInfo : paramInfos) {
                if (paramInfo instanceof CarrySetterParamInfo) {
                    ((CarrySetterParamInfo) paramInfo).setParameter(request);
                } else {
                    setterFunction.apply(methodContext).set(request, paramInfo);
                }
            }
        }
    }
}
