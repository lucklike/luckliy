package com.luckyframework.order;

import com.luckyframework.common.TempPair;
import com.luckyframework.common.TempTriple;
import com.luckyframework.definition.BeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.lang.reflect.Method;
import java.util.Comparator;

import static com.luckyframework.scanner.Constants.ORDER_ANNOTATION_NAME;
import static com.luckyframework.scanner.Constants.PRIORITY_DESTROY_ANNOTATION_NAME;

/**
 * 排序相关
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/16 上午11:53
 */
public class OrderRelated {

    public static boolean isOrderMethod(Method method) {
        return AnnotatedElementUtils.isAnnotated(method, ORDER_ANNOTATION_NAME) ||
                AnnotatedElementUtils.isAnnotated(method, PRIORITY_DESTROY_ANNOTATION_NAME);
    }

    public static Integer getOrder(Object obj){
        return AnnotationAwareOrderUtils.INSTANCE.getOrderInt(obj);
    }

    public static class TempPairOrderComparator<S,E> implements Comparator<TempPair<S,E>> {
        @Override
        public int compare(TempPair<S, E> o1, TempPair<S, E> o2) {
            return new AnnotationAwareOrderComparator().compare(o1.getOne(),o2.getOne());
        }
    }

    public static class TempTripleOrderComparator<S,T,E> implements Comparator<TempTriple<S,T,E>>{

        @Override
        public int compare(TempTriple<S, T, E> o1, TempTriple<S, T, E> o2) {
            return new AnnotationAwareOrderComparator().compare(o1.getOne(),o2.getOne());
        }
    }

    public static class BeanDefinitionOrderComparator implements Comparator<BeanDefinition>{
        @Override
        public int compare(BeanDefinition o1, BeanDefinition o2) {
            return Integer.compare(o1.getPriority(),o2.getPriority());
        }
    }

    public static class AnnotationAwareOrderUtils extends AnnotationAwareOrderComparator{

        public static final AnnotationAwareOrderUtils INSTANCE = new AnnotationAwareOrderUtils();

        public Integer getOrderInt(Object object){
            Integer order = findOrder(object);
            return order == null ? Ordered.LOWEST_PRECEDENCE : order;
        }


    }
}
