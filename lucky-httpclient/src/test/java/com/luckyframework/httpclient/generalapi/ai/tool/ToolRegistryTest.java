package com.luckyframework.httpclient.generalapi.ai.tool;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 工具注册表的离线断言：同名冲突、覆盖开关、子集快照、列举顺序、按名查询。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolRegistryTest {

    private static ToolSpec spec(String name) {
        return new ToolSpec(name, "工具" + name, ToolSpec.emptyParameters());
    }

    private static ToolRegistry registryWithThreeTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("a"), args -> "A");
        registry.register(spec("b"), args -> "B");
        registry.register(spec("c"));
        return registry;
    }

    @Test
    public void duplicateNameIsNotSilentlyOverwritten() {
        ToolRegistry registry = new ToolRegistry();
        ToolExecutor first = args -> "first";
        registry.register(spec("dup"), first);

        try {
            registry.register(spec("dup"), args -> "second");
            fail("Expected a ToolRegistryException for the duplicated tool name.");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("dup"));
        }

        assertSame(first, registry.findExecutor("dup").orElse(null));
        assertEquals("first", callExecutor(registry, "dup"));
        assertEquals(1, registry.size());
    }

    @Test
    public void overwriteSwitchReplacesRegistration() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("dup"), args -> "first");
        registry.register(spec("dup"), args -> "second", true);

        assertEquals("second", callExecutor(registry, "dup"));
        assertEquals(1, registry.size());
    }

    @Test
    public void selectReturnsSnapshotWithoutTouchingSource() {
        ToolRegistry origin = registryWithThreeTools();
        ToolRegistry subset = origin.select("b");

        assertNotSame(origin, subset);
        assertEquals(1, subset.size());
        assertEquals(Collections.singletonList("b"), names(subset.listSpecs()));
        assertFalse(subset.contains("a"));
        assertFalse(subset.contains("c"));

        assertEquals(3, origin.size());
        assertEquals(Arrays.asList("a", "b", "c"), names(origin.listSpecs()));

        try {
            origin.select("notRegistered");
            fail("Expected a ToolRegistryException for the unregistered name.");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("notRegistered"));
        }
        assertEquals(3, origin.size());
    }

    @Test
    public void listingOrderIsStable() {
        ToolRegistry registry = registryWithThreeTools();
        List<String> first = names(registry.listSpecs());
        List<String> second = names(registry.listSpecs());
        assertEquals(Arrays.asList("a", "b", "c"), first);
        assertEquals(first, second);
    }

    @Test
    public void lookupByUnknownNameGivesExplicitResult() {
        ToolRegistry registry = registryWithThreeTools();
        Optional<ToolSpec> found = registry.findSpec("a");
        assertTrue(found.isPresent());
        assertEquals("a", found.get().getName());

        assertFalse(registry.findSpec("unknown").isPresent());
        assertFalse(registry.findExecutor("c").isPresent());
        try {
            registry.getSpec("unknown");
            fail("Expected a ToolRegistryException for the unknown tool name.");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("unknown"));
        }
    }

    @Test
    public void defaultRegistryIsSharedInstance() {
        assertSame(ToolRegistry.getDefault(), ToolRegistry.getDefault());
    }

    private static List<String> names(List<ToolSpec> specs) {
        List<String> names = new ArrayList<>(specs.size());
        for (ToolSpec spec : specs) {
            names.add(spec.getName());
        }
        return names;
    }

    private static String callExecutor(ToolRegistry registry, String name) {
        try {
            return String.valueOf(registry.findExecutor(name).orElseThrow(IllegalStateException::new)
                    .execute(Collections.<String, Object>emptyMap()));
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
