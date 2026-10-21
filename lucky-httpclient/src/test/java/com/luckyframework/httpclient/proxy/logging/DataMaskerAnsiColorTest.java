package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.FontUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 彩色日志文本按键名脱敏测试
 * <pre>
 * 日志处理器为键名/值分别着色并按列对齐输出，键与分隔符之间会夹杂ANSI控制码与对齐填充空格，
 * 本测试锁定"带色文本的按键名脱敏"能力：
 *   1.字段名与值都能正确识别，不受色码与填充空格影响
 *   2.脱敏只替换可见字符，原有ANSI色码/结构字符原样保留
 *   3.未命中脱敏器的行整体原样返回
 * </pre>
 */
public class DataMaskerAnsiColorTest {

    private static final String TOKEN = "tk_live_9f8e7d6c5b4a3210";
    private static final String BASIC_AUTH = "Basic ZGVtby1qd3QtYmVhcmVyOnRLc3Qtc2VjcmV0";

    @Before
    public void setUp() {
        DataMasker.clearMaskers();
        DataMasker.clearValueMaskers();
    }

    @After
    public void tearDown() {
        DataMasker.clearMaskers();
        DataMasker.clearValueMaskers();
    }

    /**
     * 请求头：名称红色、冒号红色、值浅灰（与BeautifulLoggerPrintHandler的输出格式一致）
     */
    private String coloredRequestHeader(String name, String value) {
        return "\t" + FontUtil.getRedStr(name) + "   " + FontUtil.getRedStr(":") + " " + FontUtil.getLightGrayStr(value);
    }

    /**
     * 响应头：三列均为浅灰色
     */
    private String coloredResponseHeader(String name, String value) {
        return "\t" + FontUtil.getLightGrayStr(name) + "   " + FontUtil.getLightGrayStr(":") + " " + FontUtil.getLightGrayStr(value);
    }

    /**
     * 场景1：彩色请求头按键名脱敏，色码与结构字符原样保留
     */
    @Test
    public void shouldMaskColoredRequestHeaderAndKeepAnsiCodes() {
        DataMasker.addMasker("Authorization", MaskType.FIRST6_LAST4);

        String masked = DataMasker.maskSensitiveData(coloredRequestHeader("Authorization", BASIC_AUTH));

        assertFalse("授权头原始值不应出现", masked.contains(BASIC_AUTH));
        assertTrue("脱敏值应嵌入原有色码中", masked.contains(FontUtil.getLightGrayStr(MaskType.FIRST6_LAST4.mask(BASIC_AUTH))));
        assertTrue("名称列色码应保留", masked.contains(FontUtil.getRedStr("Authorization")));
        assertTrue("冒号列色码应保留", masked.contains(FontUtil.getRedStr(":")));
    }

    /**
     * 场景2：值中包含空格时整体脱敏（不能只脱敏第一个片段）
     */
    @Test
    public void shouldMaskWholeHeaderValueContainingWhitespace() {
        DataMasker.addMasker("Authorization", MaskType.FIRST6_LAST4);

        String masked = DataMasker.maskSensitiveData(coloredRequestHeader("Authorization", BASIC_AUTH));

        assertFalse("凭据部分不应残留", masked.contains("ZGVtby1qd3QtYmVhcmVyOnRLc3Qtc2VjcmV0"));
    }

    /**
     * 场景3：彩色响应头脱敏 + 极短值退化为完全脱敏
     */
    @Test
    public void shouldMaskColoredResponseHeader() {
        DataMasker.addMasker("Expires", MaskType.FIRST6_LAST4);
        DataMasker.addMasker("X-Auth-Token", MaskType.FULL);

        String masked = DataMasker.maskSensitiveData(
                coloredResponseHeader("Expires", "0") + "\n" + coloredResponseHeader("X-Auth-Token", TOKEN));

        assertTrue("极短值应退化为完全脱敏", masked.contains(FontUtil.getLightGrayStr("********")));
        assertFalse("令牌原始值不应出现", masked.contains(TOKEN));
    }

    /**
     * 场景4：正则字段名同样作用于彩色文本；未命中脱敏器的行原样返回
     */
    @Test
    public void shouldSupportRegexKeyAndKeepUnmatchedLine() {
        DataMasker.addMasker("(?i).*token.*", MaskType.FIRST3_LAST4);

        String matched = DataMasker.maskSensitiveData(coloredRequestHeader("X-Api-Token", TOKEN));
        assertFalse("正则字段名命中的令牌不应出现", matched.contains(TOKEN));
        assertTrue("正则字段名命中的令牌应被脱敏", matched.contains(FontUtil.getLightGrayStr(MaskType.FIRST3_LAST4.mask(TOKEN))));

        String unmatchedLine = coloredRequestHeader("X-Trace-Id", "trace-20261015-0001");
        assertEquals("未命中脱敏器的行应原样返回", unmatchedLine, DataMasker.maskSensitiveData(unmatchedLine));
    }
}
