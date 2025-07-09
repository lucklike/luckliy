package com.luckyframework.spel;

import org.springframework.expression.EvaluationException;

import java.util.HashMap;
import java.util.Map;

/**
 * 支持别名的限制类型的定位器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/7/9 10:27
 */
public class SupportAliasesRestrictedTypeLocator extends RestrictedTypeLocator {

    private final Map<String, String> aliases = new HashMap<>();

    public void addAlias(String alias, String type) {
        if (!alias.contains(".") && Character.isLowerCase(alias.charAt(0))) {
            throw new AliasException("Illegal type alias：{}", alias);
        }
        if (aliases.containsKey(alias)) {
            throw new AliasException("The type alias with the name {} already exists.", alias);
        }
        aliases.put(alias, type);
    }

    public void addAlias(String alias, Class<?> type) {
        addAlias(alias, type.getName());
    }

    public void removeAlias(String alias) {
       aliases.remove(alias);
    }

    public void merge(SupportAliasesRestrictedTypeLocator restrictedTypeLocator) {
        aliases.putAll(restrictedTypeLocator.aliases);
        super.merge(restrictedTypeLocator);
    }

    @Override
    public Class<?> findType(String typeName) throws EvaluationException {
        return super.findType(aliasConvert(typeName));
    }

    private String aliasConvert(String aliasName) {
        return aliases.getOrDefault(aliasName, aliasName);
    }
}
