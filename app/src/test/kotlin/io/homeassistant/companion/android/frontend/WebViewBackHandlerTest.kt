package io.homeassistant.companion.android.frontend

import android.net.Uri
import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WebViewBackHandlerTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `resolveBackAction returns Exit when no history and root URL`() {
        val webView = WebView(context)
        val loadedUrl = Uri.parse("https://ha.local:8123/?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertEquals(BackAction.Exit, action)
    }

    @Test
    fun `resolveBackAction returns Exit when loadedUrl is null`() {
        val webView = WebView(context)

        val action = resolveBackAction(webView, null)

        assertEquals(BackAction.Exit, action)
    }

    @Test
    fun `resolveBackAction returns NavigateToRoot when on sub-path with no history`() {
        val webView = WebView(context)
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
        val rootUrl = (action as BackAction.NavigateToRoot).rootUrl
        assertEquals("/", rootUrl.path)
        assertEquals("1", rootUrl.getQueryParameter("external_auth"))
        assertEquals("ha.local", rootUrl.host)
    }

    @Test
    fun `resolveBackAction NavigateToRoot strips query params and fragment`() {
        val webView = WebView(context)
        val loadedUrl = Uri.parse("https://ha.local:8123/history?start_date=2026-01-01&external_auth=1#tab")

        val action = resolveBackAction(webView, loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
        val rootUrl = (action as BackAction.NavigateToRoot).rootUrl
        assertEquals("/", rootUrl.path)
        assertEquals("1", rootUrl.getQueryParameter("external_auth"))
        // Original query params and fragment should be stripped
        assertEquals(null, rootUrl.getQueryParameter("start_date"))
        assertEquals(null, rootUrl.fragment)
    }

    @Test
    fun `resolveBackAction returns Exit when on root path with fragment only`() {
        val webView = WebView(context)
        val loadedUrl = Uri.parse("https://ha.local:8123/?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertEquals(BackAction.Exit, action)
    }
}
