package com.luckyframework.annotations;

public enum ProxyMode {

    /**
     * Default typically equals {@link #NO}, unless a different default
     * has been configured at the component-scan instruction level.
     */
    DEFAULT,

    /**
     * Do not create a scoped proxy.
     * <p>This proxy-mode is not typically useful when used with a
     * non-singleton scoped instance, which should favor the use of the
     * {@link #INTERFACES} or {@link #TARGET_CLASS} proxy-modes instead if it
     * is to be used as a dependency.
     */
    NO,

    /**
     * Create a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
     * the class of the target object.
     */
    INTERFACES,

    /**
     * Create a class-based proxy (uses CGLIB).
     */
    TARGET_CLASS,

    /**
     * Support for nested proxy models (uses CGLIB).
     */
    SUPPORT_NESTED,

    /**
     * Automatically select proxy mode
     */
    AUTO

}
