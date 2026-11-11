package com.luckyframework.idea.spel.evaluator

/**
 * 受控求值的结果（design D11）。
 *
 * 所有失败路径都用独立类型表达，保证 UI 只做展示、不做判断：
 * [MissingVariable] 对应「变量未提供」，[Forbidden] 对应白名单拒绝，
 * [Timeout] 对应超时，[Unavailable] 对应项目缺少 spring-expression。
 */
sealed interface EvaluatorResult {

    /** 求值成功；[value] 已格式化为展示文本，[type] 是运行时类型名。 */
    data class Success(val value: String, val type: String) : EvaluatorResult

    /** 表达式引用了未提供模拟值的变量；[name] 带 `#` 前缀。 */
    data class MissingVariable(val name: String) : EvaluatorResult

    /** 白名单拒绝了类型或方法调用，[reason] 是面向用户的原因。 */
    data class Forbidden(val reason: String) : EvaluatorResult

    /** 求值超过限制时间；[seconds] 是限制值。 */
    data class Timeout(val seconds: Long) : EvaluatorResult

    /** 解析或执行失败；[message] 可能包含错误位置。 */
    data class Failure(val message: String) : EvaluatorResult

    /** 当前项目没有可用于求值的 spring-expression。 */
    data class Unavailable(val message: String) : EvaluatorResult
}

/** 求值引擎：`SpelEvaluator` 负责超时与结果归类，引擎只执行表达式。 */
fun interface EvaluatorEngine {
    fun evaluate(expression: String, variables: Map<String, Any?>): Any?
}

/** 白名单拒绝时由代理抛出，最终转换为 [EvaluatorResult.Forbidden]。 */
class EvaluatorForbiddenException(message: String) : RuntimeException(message)
