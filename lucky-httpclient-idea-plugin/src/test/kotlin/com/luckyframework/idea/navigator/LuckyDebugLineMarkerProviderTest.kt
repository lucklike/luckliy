package com.luckyframework.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings

/**
 * 编辑器 gutter 调试图标：
 *
 * - Lucky 组件接口的实例方法显示小飞机图标，与容器状态无关；
 * - 普通类、函数组件与静态方法都不显示。
 */
class LuckyDebugLineMarkerProviderTest : NavigatorFixtureTestCase() {

    private var originalEnabled = true

    override fun setUp() {
        super.setUp()
        originalEnabled = LuckyHttpClientSettings.getInstance().isEnabled
        LuckyHttpClientSettings.getInstance().isEnabled = true
    }

    override fun tearDown() {
        try {
            LuckyHttpClientSettings.getInstance().isEnabled = originalEnabled
        } finally {
            super.tearDown()
        }
    }

    fun testApiMethodShowsDebugIconRegardlessOfContainerState() {
        val marker = markerOf(apiMethod())
        assertNotNull(marker)
        assertSame(LuckyIcons.Send, marker!!.icon)
        assertNotNull("图标应可点击打开调试面板", marker.navigationHandler)
    }

    fun testComponentMethodWithoutHttpAnnotationShowsDebugIcon() {
        // 侧边栏对组件内全部实例方法展示调试图标，编辑器保持一致
        val psiClass = addClass(
            "WholeComponentApi",
            """
            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface WholeComponentApi {
                String fetch(String id);
            }
            """
        )
        assertSame(LuckyIcons.Send, markerOf(methodOf(psiClass, "fetch"))!!.icon)
    }

    fun testPlainClassMethodHasNoDebugIcon() {
        val psiClass = addClass(
            "PlainService",
            """
            public class PlainService {
                public String call(String id) { return id; }
            }
            """
        )
        assertNull(markerOf(methodOf(psiClass, "call")))
    }

    fun testStaticMethodHasNoDebugIcon() {
        val psiClass = addClass(
            "ApiWithHelper",
            """
            import com.luckyframework.httpclient.proxy.annotations.Get;
            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface ApiWithHelper {
                @Get("/items")
                String items();

                static String helper() { return ""; }
            }
            """
        )
        assertNull(markerOf(methodOf(psiClass, "helper")))
    }

    fun testFunctionComponentMethodHasNoDebugIcon() {
        val psiClass = addClass(
            "FunctionApi",
            """
            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;

            @LuckyComponent(type = LuckyComponentType.FUNCTION)
            public class FunctionApi {
                public String invoke(String id) { return id; }
            }
            """
        )
        assertNull(markerOf(methodOf(psiClass, "invoke")))
    }

    fun testDisabledPluginHidesDebugIcon() {
        val settings = LuckyHttpClientSettings.getInstance()
        settings.isEnabled = false
        try {
            assertNull(markerOf(apiMethod()))
        } finally {
            settings.isEnabled = true
        }
    }

    private fun apiMethod(): PsiMethod {
        val psiClass = addClass(
            "OrderApi",
            """
            import com.luckyframework.httpclient.proxy.annotations.Get;
            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface OrderApi {
                @Get("/orders/{id}")
                String getOrder(String id);
            }
            """
        )
        return methodOf(psiClass, "getOrder")
    }

    private fun markerOf(method: PsiMethod): LineMarkerInfo<*>? {
        val identifier = method.nameIdentifier ?: error("方法没有名字标识符")
        val result = ArrayList<LineMarkerInfo<*>>()
        LuckyDebugLineMarkerProvider()
            .collectSlowLineMarkers(mutableListOf<PsiElement>(identifier), result)
        return result.firstOrNull()
    }
}
