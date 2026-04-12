package io.homeassistant.companion.android.util.compose.webview

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewBackNavigationTest {

    @Test
    fun `given no previous url and root loaded url, when resolving back action, then returns None`() {
        val loadedUrl = Uri.parse("https://ha.local:8123/?external_auth=1")

        val action = resolveBackAction(previousUrl = null, loadedUrl = loadedUrl)

        assertEquals(BackAction.None, action)
    }

    @Test
    fun `given no previous url and null loaded url, when resolving back action, then returns None`() {
        val action = resolveBackAction(previousUrl = null, loadedUrl = null)

        assertEquals(BackAction.None, action)
    }

    @Test
    fun `given no previous url and sub-path loaded url, when resolving back action, then returns NavigateToRoot`() {
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(previousUrl = null, loadedUrl = loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
        val rootUrl = (action as BackAction.NavigateToRoot).rootUrl
        assertEquals("/", rootUrl.path)
        assertEquals("1", rootUrl.getQueryParameter("external_auth"))
        assertEquals("ha.local", rootUrl.host)
    }

    @Test
    fun `given sub-path loaded url with extra params, when resolving back action, then NavigateToRoot strips query and fragment`() {
        val loadedUrl = Uri.parse("https://ha.local:8123/history?start_date=2026-01-01&external_auth=1#tab")

        val action = resolveBackAction(previousUrl = null, loadedUrl = loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
        val rootUrl = (action as BackAction.NavigateToRoot).rootUrl
        assertEquals("/", rootUrl.path)
        assertEquals("1", rootUrl.getQueryParameter("external_auth"))
        assertEquals(null, rootUrl.getQueryParameter("start_date"))
        assertEquals(null, rootUrl.fragment)
    }

    @Test
    fun `given same-origin previous url, when resolving back action, then returns GoBack`() {
        val previousUrl = Uri.parse("https://ha.local:8123/lovelace/0")
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(previousUrl = previousUrl, loadedUrl = loadedUrl)

        assertEquals(BackAction.GoBack, action)
    }

    @Test
    fun `given cross-origin previous url and sub-path loaded url, when resolving back action, then returns NavigateToRoot`() {
        val previousUrl = Uri.parse("https://other.server:8123/lovelace/0")
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(previousUrl = previousUrl, loadedUrl = loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
    }

    @Test
    fun `given non-http previous url, when resolving back action, then does not go back`() {
        val previousUrl = Uri.parse("about:blank")
        val loadedUrl = Uri.parse("https://ha.local:8123/history?external_auth=1")

        val action = resolveBackAction(previousUrl = previousUrl, loadedUrl = loadedUrl)

        assertTrue(action is BackAction.NavigateToRoot)
    }
}
