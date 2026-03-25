package io.homeassistant.companion.android.util.compose.webview

import android.net.Uri
import android.webkit.WebView
import io.homeassistant.companion.android.util.hasNonRootPath
import io.homeassistant.companion.android.util.hasSameOrigin

/**
 * Determines the appropriate back action for a WebView based on its history and current URL.
 *
 * The resolution logic:
 * 1. If the WebView has valid back history with a same-origin previous entry,
 *    returns [BackAction.GoBack] so the user can navigate back normally.
 * 2. If there is no valid back history (empty, cross-origin, or non-HTTP entries)
 *    and the current URL has a non-root path, returns [BackAction.NavigateToRoot]
 *    so the user is taken to the home page first.
 * 3. Otherwise returns [BackAction.Exit] so the caller can finish the activity or
 *    pop the navigation stack.
 *
 * @param webView the WebView whose history is inspected
 * @param loadedUrl the current URL shown in the WebView (as tracked by the caller,
 *        not necessarily [WebView.getUrl] which can be `about:blank` during loads)
 * @return the [BackAction] that the caller should execute
 */
fun resolveBackAction(webView: WebView, loadedUrl: Uri?): BackAction {
    if (webView.canGoBack()) {
        val backForwardList = webView.copyBackForwardList()
        val currentIndex = backForwardList.currentIndex
        if (currentIndex > 0) {
            val previousUrl = Uri.parse(
                backForwardList.getItemAtIndex(currentIndex - 1).url,
            )
            // Skip about:blank and other non-HTTP entries that may appear
            // before the first real page has loaded.
            if (loadedUrl != null &&
                previousUrl.scheme?.startsWith("http") == true &&
                previousUrl.hasSameOrigin(loadedUrl)
            ) {
                return BackAction.GoBack
            }
        } else {
            return BackAction.GoBack
        }
    }

    // History is empty or previous entry has a different origin
    // (stale entry from old connection). Navigate to root URL
    // before exiting the screen.
    if (loadedUrl != null && loadedUrl.hasNonRootPath()) {
        val rootUrl = loadedUrl.buildUpon()
            .path("/")
            .clearQuery()
            .appendQueryParameter("external_auth", "1")
            .fragment(null)
            .build()
        return BackAction.NavigateToRoot(rootUrl)
    }

    return BackAction.Exit
}

/**
 * Represents the action to take when the user presses back in a WebView.
 */
sealed interface BackAction {
    /** Navigate back in the WebView history. */
    data object GoBack : BackAction

    /** Clear history and navigate to the root URL of the current server. */
    data class NavigateToRoot(val rootUrl: Uri) : BackAction

    /** No more back navigation possible — exit the screen. */
    data object Exit : BackAction
}
