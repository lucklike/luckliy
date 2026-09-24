package com.luckyframework.httpclient.proxy.logging;

/**
 * 自定义脱敏器
 * <p>
 * 使用约束（重要）：
 * <ul>
 *     <li><b>幂等性</b>：脱敏引擎会在值发生变化时才替换文本，但无法感知自定义实现对已脱敏值的再次处理，
 *     请保证实现对“已脱敏的值”再次执行不产生新的变化（例如使用掩码字符 * 的结果不应再被匹配）；</li>
 *     <li><b>线程安全</b>：实现类实例可能被多线程共享（包括单例实例），实现必须无状态或线程安全；</li>
 *     <li><b>异常安全</b>：mask 抛出异常会被上层捕获并跳过当前字段，请尽量自行处理异常；</li>
 *     <li><b>返回值</b>：返回 null 会被视为“无需替换”。</li>
 * </ul>
 */
@FunctionalInterface
public interface CustomMasker {

    /**
     * 对值执行脱敏
     *
     * @param value 原始值
     * @return 脱敏后的值；返回 null 表示无需替换
     */
    String mask(String value);
}
