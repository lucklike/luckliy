package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.Context;

import java.lang.annotation.Annotation;

public class HookContext extends AnnotationContext {

    public HookContext(Context valueContext, Annotation hookAnnotation) {
        setContext(valueContext);
        setAnnotation(hookAnnotation);
    }


}
