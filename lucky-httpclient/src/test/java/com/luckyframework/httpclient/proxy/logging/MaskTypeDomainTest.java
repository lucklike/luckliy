package com.luckyframework.httpclient.proxy.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 领域格式类脱敏测试（JWT/MAC/URL/PLATE）
 * <pre>
 * 该类脱敏先按各自的格式特征识别，命中后按键位规则脱敏；
 * 不满足格式特征时统一退化为保留前3后4脱敏，避免原始数据泄露
 * </pre>
 */
public class MaskTypeDomainTest {

    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.dBjftJeZ4CVP";

    @Test
    public void jwt() {
        assertEquals("eyJ***.eyJ***.dBj***", MaskType.JWT.mask(JWT));
        assertEquals("Bearer eyJ***.eyJ***.dBj***", MaskType.JWT.mask("Bearer " + JWT));
        assertEquals("not**-jwt", MaskType.JWT.mask("not-a-jwt"));
    }

    @Test
    public void mac() {
        assertEquals("00:*:*:*:*:5E", MaskType.MAC.mask("00:1A:2B:3C:4D:5E"));
        assertEquals("00-*-*-*-*-5E", MaskType.MAC.mask("00-1A-2B-3C-4D-5E"));
        assertEquals("********", MaskType.MAC.mask("not-mac"));
    }

    @Test
    public void url() {
        assertEquals("https://api.example.com/****", MaskType.URL.mask("https://api.example.com/v1/users?page=1&token=abc"));
        assertEquals("https://api.example.com/****", MaskType.URL.mask("https://api.example.com?token=abc"));
        assertEquals("http://****@example.com/****", MaskType.URL.mask("http://admin:123456@example.com/private/data"));
        assertEquals("https://api.example.com", MaskType.URL.mask("https://api.example.com"));
        assertEquals("not**_url", MaskType.URL.mask("not_a_url"));
    }

    @Test
    public void plate() {
        assertEquals("京A***45", MaskType.PLATE.mask("京A12345"));
        assertEquals("沪A****45", MaskType.PLATE.mask("沪AD12345"));
        assertEquals("********", MaskType.PLATE.mask("ABC"));
    }
}
