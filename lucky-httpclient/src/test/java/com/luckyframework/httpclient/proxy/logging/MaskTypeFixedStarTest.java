package com.luckyframework.httpclient.proxy.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 固定星号数量的脱敏类型测试（FIRSTx_STARy_LASTz）
 * <pre>
 * 该类脱敏的首尾保留位数与中间星号数量都是固定的，星号数量不随值长度变化；
 * 值长度不足以同时保留首尾时退化为完全脱敏
 * </pre>
 */
public class MaskTypeFixedStarTest {

    @Test
    public void first3Star4Last4() {
        assertEquals("138****5678", MaskType.FIRST3_STAR4_LAST4.mask("13812345678"));
        assertEquals("138****2345", MaskType.FIRST3_STAR4_LAST4.mask("13812345"));
        assertEquals("********", MaskType.FIRST3_STAR4_LAST4.mask("1381234"));
    }

    @Test
    public void first4Star4Last4() {
        assertEquals("dev-****5b4a", MaskType.FIRST4_STAR4_LAST4.mask("dev-9f8e7d6c5b4a"));
        assertEquals("********", MaskType.FIRST4_STAR4_LAST4.mask("dev-1234"));
    }

    @Test
    public void first6Star6Last4() {
        assertEquals("622588******8888", MaskType.FIRST6_STAR6_LAST4.mask("6225888888888888"));
        assertEquals("********", MaskType.FIRST6_STAR6_LAST4.mask("6225888888"));
    }

    @Test
    public void first6Star8Last4() {
        assertEquals("510123********1234", MaskType.FIRST6_STAR8_LAST4.mask("510123199001011234"));
        assertEquals("********", MaskType.FIRST6_STAR8_LAST4.mask("5101231234"));
    }

    @Test
    public void star4Last4() {
        assertEquals("****5678", MaskType.STAR4_LAST4.mask("13812345678"));
        assertEquals("****8888", MaskType.STAR4_LAST4.mask("6225888888888888"));
        assertEquals("********", MaskType.STAR4_LAST4.mask("5678"));
    }
}
