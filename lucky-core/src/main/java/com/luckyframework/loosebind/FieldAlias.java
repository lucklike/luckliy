package com.luckyframework.loosebind;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 属性别名
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/25 16:34
 */
public class FieldAlias {
    private final String name;
    private final Set<String> aliases = new HashSet<>();

    public FieldAlias(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public FieldAlias addAliases(String ...aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    public boolean removeAlias(String alias) {
        return this.aliases.remove(alias);
    }

}
