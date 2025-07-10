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

    /**
     * 别名配置
     */
    private final Map<String, String> aliases = new HashMap<>();

    /**
     * 添加别名配置
     *
     * @param alias 别名（如果是单个单词，首字母必须大写）
     * @param type  类型名称
     */
    public void addAlias(String alias, String type) {
        if (!alias.contains(".") && Character.isLowerCase(alias.charAt(0))) {
            throw new AliasException("Illegal type alias：{}", alias);
        }
        if (aliases.containsKey(alias)) {
            throw new AliasException("The type alias with the name {} already exists.", alias);
        }
        aliases.put(alias, type);
    }

    /**
     * 添加别名配置
     *
     * @param alias 别名（如果是单个单词，首字母必须大写）
     * @param type  类型
     */
    public void addAlias(String alias, Class<?> type) {
        addAlias(alias, type.getName());
    }

    /**
     * 移除一个别名配置
     *
     * @param alias 别名
     */
    public void removeAlias(String alias) {
        aliases.remove(alias);
    }

    /**
     * 配置合并，将传入的定位器的配置与当前配置进行合并
     *
     * @param restrictedTypeLocator 另外一个类型定位器
     */
    public void mergeConfig(SupportAliasesRestrictedTypeLocator restrictedTypeLocator) {
        aliases.putAll(restrictedTypeLocator.aliases);
        super.mergeConfig(restrictedTypeLocator);
    }

    /**
     * 类型查找
     *
     * @param typeName the type to locate
     * @return 查找结果
     * @throws EvaluationException 可能出现的异常
     */
    @Override
    public Class<?> findType(String typeName) throws EvaluationException {
        return super.findType(aliasConvert(typeName));
    }

    /**
     * 别名转换
     *
     * @param alias 别名
     * @return 类型名
     */
    private String aliasConvert(String alias) {
        return aliases.getOrDefault(alias, alias);
    }
}
