package com.luckyframework.idea.debug.invoke

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMethod
import com.luckyframework.idea.debug.protocol.DebugErrorCode
import com.luckyframework.idea.debug.protocol.DebugProtocol
import com.luckyframework.idea.debug.protocol.DebugRequest
import com.luckyframework.idea.debug.protocol.DebugResponse
import com.luckyframework.idea.debug.rpc.DebugRpcClient
import com.luckyframework.idea.debug.rpc.DebugTimeoutException

/**
 * 一次调试目标：接口方法及其参数元数据。
 *
 * [parameters] 的顺序与运行期方法签名一致，[parameterTypes] 用于 launcher
 * 定位重载；两者都由 [DebugTargetFactory] 在 read action 中一次性生成。
 */
data class DebugTarget(
    val moduleName: String,
    val className: String,
    val methodName: String,
    val parameters: List<DebugParameter>,
    val signature: String
) {

    /** 擦除泛型后的参数类型，顺序与方法签名一致。 */
    val parameterTypes: List<String> get() = parameters.map { it.typeName }

    val displayName: String get() = "$className#$methodName"
}

/**
 * PsiMethod → [DebugTarget]。
 *
 * 调用方必须已经处于 read action；参数名解析依赖注解与编译期参数名，
 * 与补全/导航栏使用同一套 [DebugParameterCollector] 规则。
 */
object DebugTargetFactory {

    fun of(module: Module, method: PsiMethod): DebugTarget? {
        val psiClass = method.containingClass ?: return null
        val className = psiClass.qualifiedName ?: return null
        val parameters = DebugParameterCollector().collect(method)
        val signature = "${method.name}(${parameters.joinToString(", ") { it.typeName }})"
        return DebugTarget(
            moduleName = module.name,
            className = className,
            methodName = method.name,
            parameters = parameters,
            signature = signature
        )
    }
}

/** 一次调试调用的结果；UI 只消费这一层，不直接接触协议帧。 */
sealed interface DebugCallOutcome {

    data class Success(
        val returnType: String?,
        val kind: String,
        val body: String,
        val elapsedMs: Long
    ) : DebugCallOutcome

    data class Failure(
        val code: DebugErrorCode,
        val message: String,
        val detail: String? = null,
        val elapsedMs: Long = 0L
    ) : DebugCallOutcome
}

/**
 * 调试调用执行器：参数编码 → invoke 帧 → 返回值格式化。
 *
 * [invoker] 注入真实的 RPC 通道（UI 里是 DebugContainerService#invoke），
 * 单测可以直接替换成固定响应。本类不依赖 IntelliJ 平台服务与 UI。
 */
class DebugCallExecutor(
    private val invoker: (DebugRequest, Long) -> DebugResponse
) {

    fun invoke(
        target: DebugTarget,
        formValues: Map<String, String> = emptyMap(),
        jsonArguments: String? = null,
        timeoutMillis: Long = DebugProtocol.DEFAULT_TIMEOUT_MILLIS
    ): DebugCallOutcome {
        val arguments = try {
            if (jsonArguments != null) {
                DebugArgumentCodec.encodeJson(jsonArguments, target.parameters)
            } else {
                DebugArgumentCodec.encodeForm(formValues, target.parameters)
            }
        } catch (e: DebugArgumentCodec.CodecException) {
            return DebugCallOutcome.Failure(DebugErrorCode.UNKNOWN, "参数格式错误", e.message)
        }

        val payload = JsonObject().apply {
            addProperty("className", target.className)
            addProperty("methodName", target.methodName)
            add("parameterTypes", JsonArray().apply {
                target.parameterTypes.forEach { add(it) }
            })
            add("arguments", arguments)
        }
        val request = DebugRequest(DebugRpcClient.newId(), DebugProtocol.OP_INVOKE, payload)

        return try {
            val response = invoker(request, timeoutMillis)
            if (response.ok) {
                successOf(response)
            } else {
                DebugCallOutcome.Failure(
                    code = response.error,
                    message = response.errorMessage?.takeIf { it.isNotBlank() } ?: "调用失败",
                    detail = response.stackTrace,
                    elapsedMs = response.elapsedMs
                )
            }
        } catch (e: DebugTimeoutException) {
            DebugCallOutcome.Failure(
                DebugErrorCode.TIMEOUT,
                "执行超时（${e.timeoutMillis}ms），目标方法可能仍在运行",
                null
            )
        } catch (e: Throwable) {
            DebugCallOutcome.Failure(
                DebugErrorCode.UNKNOWN,
                e.message ?: e.javaClass.name,
                null
            )
        }
    }

    /**
     * 返回值格式化。
     *
     * launcher 已把结果编码为 resultKind + 文本；这里只做展示层处理：
     * JSON 重新缩进、二进制给出长度、Response 展开状态与头部。
     */
    private fun successOf(response: DebugResponse): DebugCallOutcome.Success {
        val kind = response.payloadString("resultKind") ?: "unknown"
        val body = when (kind) {
            "json" -> pretty(response.payloadString("result"))
            "text" -> response.payloadString("result").orEmpty()
            "null" -> "null"
            "void" -> "void（方法没有返回值）"
            "bytes" -> bytesSummary(response)
            "response" -> pretty(response.payload.getAsJsonObject("response")?.toString())
            else -> pretty(response.payload.toString())
        }
        return DebugCallOutcome.Success(
            returnType = response.payloadString("returnType"),
            kind = kind,
            body = body,
            elapsedMs = response.elapsedMs
        )
    }

    private fun bytesSummary(response: DebugResponse): String {
        val base64 = response.payloadString("base64").orEmpty()
        val truncated = response.payloadBoolean("truncated") == true
        return buildString {
            append("Base64 长度：").append(base64.length)
            if (truncated) {
                append("（已截断）")
            }
            if (base64.isNotEmpty()) {
                append("\n\n").append(base64)
            }
        }
    }

    private fun pretty(text: String?): String {
        if (text.isNullOrBlank()) {
            return ""
        }
        return runCatching { PRETTY.toJson(JsonParser.parseString(text)) }.getOrDefault(text)
    }

    private companion object {
        private val PRETTY = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }
}
