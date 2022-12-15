package com.luckyframework.bean.aware;

import com.luckyframework.context.message.MessageSource;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/23 10:20
 */
public interface MessageSourceAware extends Aware{

    void setMessageSource(MessageSource messageSource);

}
