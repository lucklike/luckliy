package com.luckyframework.scanner;

import com.luckyframework.annotations.*;
import com.luckyframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 常量表
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/5 上午12:32
 */
public abstract class Constants {

    public final static String SCANNER_ELEMENT_ANNOTATION_NAME          = ScannerElement.class.getName();
    public final static String COMPONENT_ANNOTATION_NAME                = Component.class.getName();
    public final static String CONTROLLER_ELEMENT_ANNOTATION_NAME       = Controller.class.getName();
    public final static String SERVICE_ANNOTATION_NAME                  = Service.class.getName();
    public final static String REPOSITORY_ELEMENT_ANNOTATION_NAME       = Repository.class.getName();
    public final static String PLUGIN_ELEMENT_ANNOTATION_NAME           = Plugin.class.getName();
    public final static String CONFIGURATION_ELEMENT_ANNOTATION_NAME    = Configuration.class.getName();

    public final static String BEAN_ANNOTATION_NAME                     = Bean.class.getName();
    public final static String SCOPE_ANNOTATION_NAME                    = Scope.class.getName();
    public final static String PRIMARY_ANNOTATION_NAME                  = Primary.class.getName();
    public final static String LAZY_ANNOTATION_NAME                     = Lazy.class.getName();
    public final static String PROXY_MODEL_ANNOTATION_NAME              = ProxyModel.class.getName();
    public final static String DEPENDS_ON_ANNOTATION_NAME               = DependsOn.class.getName();
    public final static String ORDER_ANNOTATION_NAME                    = Order.class.getName();
    public final static String CONDITIONAL_ANNOTATION_NAME              = Conditional.class.getName();
    public final static String PROFILE_ANNOTATION_NAME                  = Profile.class.getName();
    public final static String PROPERTY_SOURCE_ANNOTATION_NAME          = PropertySource.class.getName();
    public final static String IMPORT_ANNOTATION_NAME                   = Import.class.getName();
    public final static String EXCLUDE_ANNOTATION_NAME                  = Exclude.class.getName();
    public final static String POST_CONSTRUCT_ANNOTATION_NAME           = PostConstruct.class.getName();
    public final static String PRE_DESTROY_ANNOTATION_NAME              = PreDestroy.class.getName();
    public final static String PRIORITY_DESTROY_ANNOTATION_NAME         = "javax.annotation.Priority";
    public final static String EVENT_LISTENER_DESTROY_ANNOTATION_NAME   = EventListener.class.getName();

    public final static String VALUE = "value";
    public final static String INIT_METHOD = "initMethod";
    public final static String DESTROY_METHOD = "destroyMethod";

    public final static String AUTOWIRE_CANDIDATE = "autowireCandidate";

}
