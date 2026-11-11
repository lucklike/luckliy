package com.luckyframework.idea.spel.metadata

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger

/**
 * 从插件资源 `spel/annotations.json` 加载注解元数据表。
 *
 * 插件运行时不反射框架注解类，只读内置 JSON；
 * 表由 `tools/generate-annotation-metadata.js` 生成并人工确认。
 */
object AnnotationMetadataLoader {

    private const val RESOURCE = "/spel/annotations.json"

    private val LOG = Logger.getInstance(AnnotationMetadataLoader::class.java)

    @Volatile
    private var cached: AnnotationTable? = null

    fun load(): AnnotationTable {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val table = read() ?: AnnotationTable.EMPTY
            cached = table
            return table
        }
    }

    private fun read(): AnnotationTable? {
        val stream = AnnotationMetadataLoader::class.java.getResourceAsStream(RESOURCE)
            ?: return run {
                LOG.warn("找不到注解元数据资源: $RESOURCE")
                null
            }
        return stream.use { input ->
            try {
                val root = JsonParser.parseReader(input.reader())?.asJsonObject ?: return null
                parse(root)
            } catch (e: Exception) {
                LOG.warn("解析注解元数据失败", e)
                null
            }
        }
    }

    private fun parse(root: JsonObject): AnnotationTable {
        val tableVersion = root.get("tableVersion")?.asString ?: "0"
        val frameworkVersion = root.get("frameworkVersion")?.asString ?: "unknown"
        val annotations = LinkedHashMap<String, SpelAnnotationMetadata>()

        root.getAsJsonObject("annotations")?.entrySet()?.forEach { (fqName, element) ->
            val attributes = LinkedHashMap<String, SpelAttributeMetadata>()
            element.asJsonObject.getAsJsonObject("attributes")?.entrySet()?.forEach { (name, meta) ->
                val objectValue = meta.asJsonObject
                attributes[name] = SpelAttributeMetadata(
                    kind = enumOrDefault(objectValue.get("kind")?.asString, SpelValueKind.UNKNOWN),
                    lifecycle = enumOrDefault(objectValue.get("lifecycle")?.asString, SpelLifecycle.METHOD)
                )
            }
            annotations[fqName] = SpelAnnotationMetadata(fqName, attributes)
        }

        return AnnotationTable(tableVersion, frameworkVersion, annotations)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T {
        if (value == null) {
            return default
        }
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}
