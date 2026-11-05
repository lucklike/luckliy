package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 能力协商断言：三家方言上报的四个标志取值与实际实现一致，未知标志查询返回不支持，
 * 并且回环引擎包里不出现厂商名字面量（上层只依据标志集合决策）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class VendorCapabilitiesTest {

    /**
     * 回环引擎源码目录（相对模块根目录，surefire的工作目录就是模块目录）
     */
    private static final String LOOP_SOURCE_DIR =
            "src/main/java/com/luckyframework/httpclient/generalapi/ai/loop";

    private static final List<VendorAdapter> DIALECTS = Arrays.asList(
            new OpenAIDialect(), new AnthropicDialect(), new GeminiDialect());

    /**
     * 三家均支持工具调用与流式工具调用，并行工具调用与schema严格模式三家均上报不支持
     */
    @Test
    public void threeDialectsReportTheSameFourFlags() {
        for (VendorAdapter dialect : DIALECTS) {
            Capabilities capabilities = dialect.capabilities();
            String vendor = dialect.getClass().getSimpleName();
            assertTrue(vendor + "必须支持工具调用", capabilities.isToolCallSupported());
            assertTrue(vendor + "必须支持流式下的工具调用", capabilities.isStreamToolCallSupported());
            assertFalse(vendor + "本轮不支持并行工具调用", capabilities.isParallelToolCallsSupported());
            assertFalse(vendor + "本轮不支持schema严格模式", capabilities.isStrictSchemaSupported());
            for (String flag : Capabilities.knownFlags()) {
                assertEqualsFlag(vendor, capabilities, flag);
            }
        }
    }

    /**
     * 逐个已知标志名的查询结果与便捷方法一致
     */
    private void assertEqualsFlag(String vendor, Capabilities capabilities, String flag) {
        boolean expected;
        if (Capabilities.TOOL_CALL.equals(flag)) {
            expected = capabilities.isToolCallSupported();
        } else if (Capabilities.STREAM_TOOL_CALL.equals(flag)) {
            expected = capabilities.isStreamToolCallSupported();
        } else if (Capabilities.PARALLEL_TOOL_CALLS.equals(flag)) {
            expected = capabilities.isParallelToolCallsSupported();
        } else {
            expected = capabilities.isStrictSchemaSupported();
        }
        assertTrue(vendor + "的标志 " + flag + " 查询结果应与便捷方法一致", capabilities.supports(flag) == expected);
    }

    /**
     * 未知标志名返回不支持而非抛异常
     */
    @Test
    public void unknownFlagIsReportedAsUnsupported() {
        for (VendorAdapter dialect : DIALECTS) {
            Capabilities capabilities = dialect.capabilities();
            assertFalse(capabilities.supports("audioStreaming"));
            assertFalse(capabilities.supports("toolCall-but-unknown"));
            assertFalse(capabilities.supports(null));
        }
    }

    /**
     * 上层不依据厂商名字符串比对：回环引擎的源码里不出现厂商名字面量与厂商分支
     */
    @Test
    public void loopEngineNeverBranchesOnVendorName() {
        File dir = new File(LOOP_SOURCE_DIR);
        assertTrue("回环引擎源码目录必须存在：" + dir.getAbsolutePath(), dir.isDirectory());
        List<String> forbidden = Arrays.asList("openai", "anthropic", "gemini", "deepseek", "vendorName");
        File[] sources = dir.listFiles((current, name) -> name.endsWith(".java"));
        assertNotNull("回环引擎源码目录里必须有java文件", sources);
        for (File source : sources) {
            String content = read(source).toLowerCase();
            for (String word : forbidden) {
                assertFalse(source.getName() + " 不该出现厂商名字面量 [" + word + "]，上层只能依据能力标志决策",
                        content.contains(word));
            }
            assertFalse(source.getName() + " 不该出现按厂商分支的判断", content.contains("if (vendor"));
        }
    }

    /**
     * 同一个会话请求交给三家方言都能得到可发送的请求体（方言选择的唯一入口是「选接口」）
     */
    @Test
    public void everyDialectCanWriteABodyForTheSameRequest() {
        ChatRequest request = AiFixtures.minimalRequest();
        for (VendorAdapter dialect : DIALECTS) {
            assertNotNull(dialect.getClass().getSimpleName() + "的请求体", dialect.writeBody(request));
        }
    }

    private static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("读取源码失败：" + file.getAbsolutePath() + "，" + e.getMessage());
            return "";
        }
    }
}
