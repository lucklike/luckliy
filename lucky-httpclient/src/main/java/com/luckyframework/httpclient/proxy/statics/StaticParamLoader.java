package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 静态参数加载器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 00:51
 */
public class StaticParamLoader {

    private final List<StaticParamAnalyzer> staticParamAnalyzers = new ArrayList<>();

    public StaticParamLoader(MethodContext context) {
        analyzerStaticParamAnnotation(context);
    }

    private void analyzerStaticParamAnnotation(MethodContext context) {
        Set<Annotation> staticParamAnnSet = context.getContainCombinationAnnotationsIgnoreSource(StaticParam.class);

        for (Annotation annotation : staticParamAnnSet) {
            // 获取静态参数注解和对象创建器
            StaticParam staticParamAnn = context.toAnnotation(annotation, StaticParam.class);
            ObjectCreator objectCreator = HttpClientProxyObjectFactory.getObjectCreator();

            staticParamAnalyzers.add(new StaticParamAnalyzer(
                    () -> (ParameterSetter) objectCreator.newObject(staticParamAnn.setter(), context),
                    () -> (StaticParamResolver) objectCreator.newObject(staticParamAnn.resolver(), context),
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
        private final Supplier<ParameterSetter> setterSupplier;
        private final Supplier<StaticParamResolver> resolverSupplier;
        private final Annotation staticParamAnnotation;

        StaticParamAnalyzer(Supplier<ParameterSetter> setterSupplier, Supplier<StaticParamResolver> resolverSupplier, Annotation staticParamAnnotation) {
            this.setterSupplier = setterSupplier;
            this.resolverSupplier = resolverSupplier;
            this.staticParamAnnotation = staticParamAnnotation;
        }

        void resolverAndSetter(Request request, MethodContext context) {
            List<ParamInfo> paramInfos = resolverSupplier.get().parser(new StaticParamAnnContext(context, staticParamAnnotation));
            for (ParamInfo paramInfo : paramInfos) {
                setterSupplier.get().set(request, paramInfo);
            }
        }
    }

}
