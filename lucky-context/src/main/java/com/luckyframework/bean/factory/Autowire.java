package com.luckyframework.bean.factory;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/22 上午1:41
 */
public enum Autowire {

    /**
     * Constant that indicates autowiring bean properties by name.
     */
    BY_NAME,

    /**
     * Constant that indicates autowiring bean properties by type.
     */
    BY_TYPE,

    /**
     * Constant that indicates autowiring bean properties by value.
     */
    BY_VALUE,

    /**
     * Automatic selection of name first type second
     */
    AUTO_NAME_FIRST,

    /**
     * Automatic selection of type first name second
     */
    AUTO_TYPE_FIRST

}
