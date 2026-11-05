package com.luckyframework.httpclient.generalapi.ai.vendor;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 能力标志的离线断言：未知标志名返回不支持而非抛异常。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class CapabilitiesTest {

    @Test
    public void unknownFlagIsReportedAsUnsupported() {
        Capabilities capabilities = Capabilities.standard();
        assertFalse(capabilities.supports("reasoning"));
        assertFalse(capabilities.supports("web_search"));
        assertFalse(capabilities.supports(null));
    }

    @Test
    public void knownFlagsReflectWhatIsEnabled() {
        Capabilities capabilities = Capabilities.standard();
        assertTrue(capabilities.supports(Capabilities.TOOL_CALL));
        assertTrue(capabilities.isToolCallSupported());
        assertTrue(capabilities.isStreamToolCallSupported());
        assertFalse(capabilities.isParallelToolCallsSupported());
        assertFalse(capabilities.isStrictSchemaSupported());

        Capabilities everything = Capabilities.of(Capabilities.knownFlags().toArray(new String[0]));
        assertTrue(everything.isParallelToolCallsSupported());
        assertTrue(everything.isStrictSchemaSupported());

        assertTrue(Capabilities.of().toString().contains("Capabilities"));
    }
}
