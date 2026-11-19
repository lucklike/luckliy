package com.luckyframework.idea.debug.protocol

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 一条调试请求。
 *
 * payload 的字段直接平铺到帧顶层（如 className/methodName），
 * 与 launcher 侧的读取方式保持一致。
 */
class DebugRequest(
    val id: String,
    val op: String,
    val payload: JsonObject = JsonObject()
) {
    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("id", id)
        addProperty("op", op)
        addProperty("protocolVersion", DebugProtocol.VERSION)
        payload.entrySet().forEach { (key, value) -> add(key, value) }
    }
}

/**
 * 一条调试响应。
 *
 * elapsedMs 由 launcher 在发送前统一写入；连接断开等异常路径可能缺失，
 * 因此解析时允许缺省为 0。
 */
class DebugResponse(
    val id: String,
    val op: String,
    val ok: Boolean,
    val errorCode: String?,
    val errorMessage: String?,
    val stackTrace: String?,
    val elapsedMs: Long,
    val payload: JsonObject
) {
    val error: DebugErrorCode
        get() = DebugErrorCode.of(errorCode)

    fun payloadString(name: String): String? = payload.stringOrNull(name)

    fun payloadBoolean(name: String): Boolean? =
        payload.get(name)?.takeIf { !it.isJsonNull }?.asBoolean

    companion object {
        fun parse(line: String): DebugResponse {
            val json = JsonParser.parseString(line).asJsonObject
            return DebugResponse(
                id = json.string("id"),
                op = json.string("op"),
                ok = json.get("ok")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                errorCode = json.stringOrNull("errorCode"),
                errorMessage = json.stringOrNull("errorMessage"),
                stackTrace = json.stringOrNull("stackTrace"),
                elapsedMs = json.get("elapsedMs")?.takeIf { !it.isJsonNull }?.asLong ?: 0L,
                payload = json.getAsJsonObject("payload") ?: JsonObject()
            )
        }
    }
}

internal fun JsonObject.string(name: String): String =
    stringOrNull(name) ?: ""

internal fun JsonObject.stringOrNull(name: String): String? {
    val value: JsonElement = get(name) ?: return null
    return if (value.isJsonNull) null else value.asString
}

internal fun JsonObject.objectOrNull(name: String): JsonObject? {
    val value: JsonElement = get(name) ?: return null
    return if (value.isJsonObject) value.asJsonObject else null
}
