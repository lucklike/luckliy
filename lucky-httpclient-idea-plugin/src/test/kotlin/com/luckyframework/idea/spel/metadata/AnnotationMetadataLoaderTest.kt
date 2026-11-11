package com.luckyframework.idea.spel.metadata

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AnnotationMetadataLoaderTest : BasePlatformTestCase() {

    fun testLoadsBuiltinTable() {
        val table = AnnotationMetadataLoader.load()

        assertTrue(table.tableVersion.isNotBlank())
        assertTrue(table.frameworkVersion.isNotBlank())
        assertTrue(table.all().isNotEmpty())
    }

    fun testHttpRequestUrlIsTemplate() {
        val metadata = AnnotationMetadataLoader.load()
            .attribute("com.luckyframework.httpclient.proxy.annotations.HttpRequest", "url")

        assertNotNull(metadata)
        assertEquals(SpelValueKind.TEMPLATE, metadata!!.kind)
        assertEquals(SpelLifecycle.REQUEST_INIT, metadata.lifecycle)
    }

    fun testSpelImportVarIsKeyValue() {
        val metadata = AnnotationMetadataLoader.load()
            .attribute("com.luckyframework.httpclient.proxy.spel.SpELImport", "var")

        assertNotNull(metadata)
        assertEquals(SpelValueKind.KEY_VALUE, metadata!!.kind)
    }
}
