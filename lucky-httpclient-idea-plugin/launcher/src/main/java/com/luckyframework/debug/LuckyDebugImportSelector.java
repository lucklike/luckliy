package com.luckyframework.debug;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 按类名补注册 starter 的装配类。
 *
 * 用 ImportSelector 而不是直接 @Import，是为了不在 launcher 的编译期
 * 引用 starter —— 用户项目可能装载的是其它版本，甚至完全没有 starter。
 * 缺失的类直接跳过，让容器以「没有工厂 bean」的状态启动，
 * 由 invoke 阶段返回 BEAN_MISSING 并给出可读提示。
 */
public class LuckyDebugImportSelector implements ImportSelector {

    private static final String[] CANDIDATES = {
            "io.github.lucklike.httpclient.LuckyHttpAutoConfiguration",
            "org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration",
            "io.github.lucklike.httpclient.discovery.ServerDiscoveryAutoConfiguration",
            "io.github.lucklike.httpclient.LuckyAutoInjectionAutoConfiguration"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        List<String> imports = new ArrayList<String>(CANDIDATES.length);
        for (String candidate : CANDIDATES) {
            if (ClassUtils.isPresent(candidate, loader)) {
                imports.add(candidate);
            }
        }
        return imports.toArray(new String[0]);
    }
}
