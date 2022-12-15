package com.luckyframework.aop.aspectj;

import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.ShadowMatch;

import java.lang.reflect.Method;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/13 0013 11:03
 */
public class AspectJExpressionPointcut extends AbstractExpressionPointcut {

    //获得切点解析器
    private static final PointcutParser pointcutParser=PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingContextClassloaderForResolution(
            PointcutParser.getAllSupportedPointcutPrimitives()
    );
    //AspectJ的Pointcut表达式校验
    private final PointcutExpression pointcutExpression;

    public AspectJExpressionPointcut(String expression) {
        super(expression);
        //表达式
        this.pointcutExpression = pointcutParser.parsePointcutExpression(expression);
    }

    @Override
    public boolean matchClass(String currentBeanName,Class<?> targetClass) {
        return pointcutExpression.couldMatchJoinPointsInType(targetClass);
    }

    @Override
    public boolean matchMethod(Class<?> targetClass, Method method, Object... args) {
        ShadowMatch sm = pointcutExpression.matchesMethodExecution(method);
        return sm.alwaysMatches();
    }
}
