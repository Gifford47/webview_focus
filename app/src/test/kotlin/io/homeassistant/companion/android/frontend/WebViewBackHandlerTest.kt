package io.homeassistant.companion.android.frontend

import android.net.Uri
import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem
import android.webkit.WebView
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewBackHandlerTest {

    private fun mockWebView(canGoBack: Boolean = false): WebView = mockk {
        every { this@mockk.canGoBack() } returns canGoBack
    }

    @Test
    fun `resolveBackAction returns Exit when no history and root URL`() {
        val webView = mockWebView(canGoBack = false)
        val loadedUrl = Uri.parse("https://ha.local:8123/?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertEquals(BackAction.Exit, action)
    }

    @Test
    fun `resolveBackAction returns Exit when loadedUrl is null`() {
        val webView = mockWebView(canGoBack = false)

        val action = resolveBackAction(webView, null)

        assertEquals(BackAction.Exit, action)
    }

    @Test
    fun `resolveBackAction returns NavigateToRoot when on sub-path with no history`() {
        val webView = mockWebView(canGoBack = false)
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
        val webView = mockWebView(canGoBack = false)
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
        val webView = mockWebView(canGoBack = false)
        val loadedUrl = Uri.parse("https://ha.local:8123/?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertEquals(BackAction.Exit, action)
    }

    @Test
    fun `resolveBackAction returns GoBack when history has same-origin previous entry`() {
        val previousItem = mockk<WebHistoryItem> {
            every { url } returns "https://ha.local:8123/lovelace/0"
        }
        val backForwardList = mockk<WebBackForwardList> {
            every { currentIndex } returns 1
            every { getItemAtIndex(0) } returns previousItem
        }
        val webView = mockk<WebView> {
            every { canGoBack() } returns true
            every { copyBackForwardList() } returns backForwardList
        }
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertEquals(BackAction.GoBack, action)
    }

    @Test
    fun `resolveBackAction returns NavigateToRoot when history has cross-origin previous entry`() {
        val previousItem = mockk<WebHistoryItem> {
            every { url } returns "https://other.server:8123/lovelace/0"
        }
        val backForwardList = mockk<WebBackForwardList> {
            every { currentIndex } returns 1
            every { getItemAtIndex(0) } returns previousItem
        }
        val webView = mockk<WebView> {
            every { canGoBack() } returns true
            every { copyBackForwardList() } returns backForwardList
        }
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(webView, loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
    }
}
