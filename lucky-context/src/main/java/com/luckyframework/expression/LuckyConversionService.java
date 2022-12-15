package com.luckyframework.expression;

import com.luckyframework.conversion.ConversionUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/17 23:32
 */
public class LuckyConversionService implements ConversionService {
    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        return true;
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return true;
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        return ConversionUtils.conversion(source, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.conversion(source, targetType.getResolvableType());
    }
}
