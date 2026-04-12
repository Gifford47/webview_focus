package io.homeassistant.companion.android.util.compose.webview

import android.net.Uri
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import io.homeassistant.companion.android.util.hasNonRootPath
import io.homeassistant.companion.android.util.hasSameOrigin

/**
 * Convenience overload that extracts the previous URL from the [WebView]'s
 * back/forward list and delegates to the pure [resolveBackAction] function.
 */
fun resolveBackAction(webView: WebView, loadedUrl: Uri?): BackAction {
    val previousUrl = if (webView.canGoBack()) {
        val backForwardList = webView.copyBackForwardList()
        backForwardList.currentIndex
            .takeIf { it > 0 }
            ?.let { backForwardList.getItemAtIndex(it - 1).url }
            ?.toUri()
    } else {
        null
    }
    return resolveBackAction(previousUrl, loadedUrl)
}

/**
 * Determines the appropriate back action based on the previous and current URL.
 *
 * The resolution logic:
 * 1. If [previousUrl] is a same-origin HTTP entry relative to [loadedUrl],
 *    returns [BackAction.GoBack] so the user can navigate back normally.
 * 2. If there is no valid previous URL and the current URL has a non-root path,
 *    returns [BackAction.NavigateToRoot] so the user is taken to the home page first.
 * 3. Otherwise returns [BackAction.None] — the caller decides what to do
 *    (e.g. exit the activity or pop the navigation stack).
 *
 * @param previousUrl the URL of the previous entry in the WebView's back stack,
 *        or `null` if the history is empty or the WebView cannot go back
 * @param loadedUrl the current URL shown in the WebView (as tracked by the caller,
 *        not necessarily [WebView.getUrl] which can be `about:blank` during loads)
 * @return the [BackAction] that the caller should execute
 */
@VisibleForTesting
internal fun resolveBackAction(previousUrl: Uri?, loadedUrl: Uri?): BackAction {
    if (previousUrl != null &&
        loadedUrl != null &&
        previousUrl.scheme?.startsWith("http") == true &&
        previousUrl.hasSameOrigin(loadedUrl)
    ) {
        return BackAction.GoBack
    }

    if (loadedUrl != null && loadedUrl.hasNonRootPath()) {
        val rootUrl = loadedUrl.buildUpon()
            .path("/")
            .clearQuery()
            .appendQueryParameter("external_auth", "1")
            .fragment(null)
            .build()
        return BackAction.NavigateToRoot(rootUrl)
    }

    return BackAction.None
}

/**
 * Represents the action to take when the user presses back in a WebView.
 */
sealed interface BackAction {
    /** Navigate back in the WebView history. */
    data object GoBack : BackAction

    /** Clear history and navigate to the root URL of the current server. */
    data class NavigateToRoot(val rootUrl: Uri) : BackAction

    /** No more back navigation possible — the caller decides what to do. */
    data object None : BackAction
}
