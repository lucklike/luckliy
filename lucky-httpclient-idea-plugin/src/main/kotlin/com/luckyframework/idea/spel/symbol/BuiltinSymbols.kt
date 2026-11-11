package com.luckyframework.idea.spel.symbol

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 内置符号表中的变量（`InternalRootVarName` / `InternalVarName`）。
 */
data class BuiltinVariable(
    val name: String,
    val constant: String?,
    val kind: SpelSymbolKind,
    val type: String,
    val stage: SpelLifecycle,
    val description: String
)

/**
 * 内置符号表中的函数（`function` 包的 public static 方法）。
 *
 * [registered] 表示该方法所在类由 `HttpClientProxyObjectFactory` 在启动时注册；
 * 未注册类中的方法需要用户通过 `@SpELImport` 导入后才可用。
 */
data class BuiltinFunction(
    val name: String,
    val method: String,
    val owner: String,
    val namespace: String?,
    val signature: String,
    val returnType: String,
    val description: String,
    val registered: Boolean
)

/**
 * 插件内置符号表与当前项目框架版本的匹配结果。
 */
enum class BuiltinVersionStatus {
    VERIFIED,
    UNVERIFIED,
    UNKNOWN
}

/**
 * 由 `tools/generate-builtin-symbols.js` 生成的内置符号表（design D5）。
 *
 * 表中数据来自框架源码，插件运行时不加载框架类；版本不匹配只提示，不阻断解析。
 */
class BuiltinSymbolsTable(
    val tableVersion: String,
    val frameworkVersion: String,
    val rootVariables: List<BuiltinVariable>,
    val variables: List<BuiltinVariable>,
    val functions: List<BuiltinFunction>
) {

    fun symbols(): List<SpelSymbol> {
        val result = ArrayList<SpelSymbol>(rootVariables.size + variables.size + functions.size)
        rootVariables.mapTo(result) { it.toSymbol() }
        variables.mapTo(result) { it.toSymbol() }
        functions.mapTo(result) { it.toSymbol() }
        return result
    }

    /**
     * 内置表按 [frameworkVersion]（如 `3.0.x`）生成；`3.0.*` 视为已验证。
     */
    fun versionStatus(detectedVersion: String?): BuiltinVersionStatus {
        val detected = detectedVersion?.trim().orEmpty()
        if (detected.isEmpty()) {
            return BuiltinVersionStatus.UNKNOWN
        }
        val prefix = frameworkVersion.removeSuffix(".x")
        return if (detected == frameworkVersion || detected.startsWith("$prefix.")) {
            BuiltinVersionStatus.VERIFIED
        } else {
            BuiltinVersionStatus.UNVERIFIED
        }
    }

    /** 未验证版本提示；版本匹配时返回 null。 */
    fun versionMessage(detectedVersion: String?): String? = when (versionStatus(detectedVersion)) {
        BuiltinVersionStatus.VERIFIED -> null
        BuiltinVersionStatus.UNVERIFIED ->
            "当前 lucky-httpclient 版本 $detectedVersion 未经验证，内置符号表基于 $frameworkVersion（未验证版本）"
        BuiltinVersionStatus.UNKNOWN ->
            "未检测到 lucky-httpclient 版本，内置符号表基于 $frameworkVersion（未验证版本）"
    }

    private fun BuiltinVariable.toSymbol(): SpelSymbol = SpelSymbol(
        name = name,
        kind = kind,
        type = type,
        origin = SpelSymbolOrigin.BUILTIN,
        lifecycle = stage,
        description = description
    )

    private fun BuiltinFunction.toSymbol(): SpelSymbol = SpelSymbol(
        name = name,
        kind = SpelSymbolKind.FUNCTION,
        type = returnType,
        origin = SpelSymbolOrigin.BUILTIN,
        lifecycle = SpelLifecycle.REQUEST_INIT,
        signature = signature,
        namespace = namespace?.takeIf { it.isNotBlank() },
        owner = owner.takeIf { it.isNotBlank() },
        description = description,
        registered = registered,
        methodName = method.takeIf { it.isNotBlank() }
    )

    companion object {
        val EMPTY = BuiltinSymbolsTable("0", "unknown", emptyList(), emptyList(), emptyList())
    }
}

/**
 * 从插件资源 `spel/builtin-symbols.json` 加载内置符号表。
 */
object BuiltinSymbolsLoader {

    private const val RESOURCE = "/spel/builtin-symbols.json"

    private val LOG = Logger.getInstance(BuiltinSymbolsLoader::class.java)

    @Volatile
    private var cached: BuiltinSymbolsTable? = null

    fun load(): BuiltinSymbolsTable {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val table = read() ?: BuiltinSymbolsTable.EMPTY
            cached = table
            return table
        }
    }

    fun source(table: BuiltinSymbolsTable = load()): SpelSymbolSource = BuiltinSpelSymbolSource(table)

    private fun read(): BuiltinSymbolsTable? {
        val stream = BuiltinSymbolsLoader::class.java.getResourceAsStream(RESOURCE)
            ?: return run {
                LOG.warn("找不到内置符号资源: $RESOURCE")
                null
            }
        return stream.use { input ->
            try {
                val root = JsonParser.parseReader(input.reader())?.asJsonObject ?: return null
                parse(root)
            } catch (e: Exception) {
                LOG.warn("解析内置符号表失败", e)
                null
            }
        }
    }

    private fun parse(root: JsonObject): BuiltinSymbolsTable {
        val tableVersion = root.get("tableVersion")?.asString ?: "0"
        val frameworkVersion = root.get("frameworkVersion")?.asString ?: "unknown"
        val rootVariables = parseVariables(root, "rootVariables", SpelSymbolKind.ROOT_VARIABLE)
        val variables = parseVariables(root, "variables", SpelSymbolKind.VARIABLE)
        val functions = ArrayList<BuiltinFunction>()
        root.getAsJsonArray("functions")?.forEach { element ->
            val objectValue = element.asJsonObject
            functions += BuiltinFunction(
                name = objectValue.string("name") ?: "",
                method = objectValue.string("method") ?: "",
                owner = objectValue.string("owner") ?: "",
                namespace = objectValue.string("namespace"),
                signature = objectValue.string("signature") ?: "",
                returnType = objectValue.string("returnType") ?: "Object",
                description = objectValue.string("description") ?: "",
                registered = objectValue.get("registered")?.asBoolean ?: false
            )
        }
        return BuiltinSymbolsTable(
            tableVersion,
            frameworkVersion,
            rootVariables,
            variables,
            functions
        )
    }

    private fun parseVariables(
        root: JsonObject,
        member: String,
        kind: SpelSymbolKind
    ): List<BuiltinVariable> {
        val result = ArrayList<BuiltinVariable>()
        root.getAsJsonArray(member)?.forEach { element ->
            val objectValue = element.asJsonObject
            result += BuiltinVariable(
                name = objectValue.string("name") ?: "",
                constant = objectValue.string("constant"),
                kind = kind,
                type = objectValue.string("type") ?: "Object",
                stage = enumOrDefault(objectValue.string("stage"), SpelLifecycle.REQUEST_INIT),
                description = objectValue.string("description") ?: ""
            )
        }
        return result
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeIf { !it.isJsonNull }?.asString

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T {
        if (value.isNullOrBlank()) {
            return default
        }
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}

/**
 * 把内置符号表包装为 [SpelSymbolSource]，供 [SpelContextBuilder] 使用。
 */
class BuiltinSpelSymbolSource(private val table: BuiltinSymbolsTable) : SpelSymbolSource {
    override fun symbols(): List<SpelSymbol> = table.symbols()
}
