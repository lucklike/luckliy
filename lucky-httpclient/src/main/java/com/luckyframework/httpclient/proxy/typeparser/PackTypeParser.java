package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

/**
 * 包装类型解析器
 * <p>
 * 按照注册顺序取第一个{@link #canHandle(MethodContext)}返回true的解析器来处理当前方法，
 * 默认解析器先于用户后续追加的解析器，因此自定义解析器的优先级最低，
 * 需要更高优先级时可以手动调整解析器列表的顺序。
 */
public interface PackTypeParser {


    /**
     * 当前处理器是否可以处理当前的方法上下文
     *
     * @param mc 方法上下文
     * @return 是否可以处理
     */
    boolean canHandle(MethodContext mc);


    /**
     * 获取包装类型中的真实类型
     *
     * @param mc       方法上下文
     * @param packType 包装类型
     * @return 真实类型
     */
    ResolvableType getRealType(MethodContext mc, ResolvableType packType);


    /**
     * 将真实对象包装成指定的包装类型
     *
     * @param mc       方法上下文
     * @param supplier 获取真实对象的方法
     * @return 包装类型对应的对象
     */
    Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable;


}
