package com.zentora.app.goalshift.passage

import android.content.Intent
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

internal class ReadingNavigation(
    private val firstLoadFinished: () -> Unit,
    private val rendererLost: () -> Unit
) : WebViewClient() {
    private var initialLoad = true

    override fun onPageFinished(view: WebView, url: String?) {
        if (initialLoad) {
            initialLoad = false
            firstLoadFinished()
        }
        CookieManager.getInstance().flush()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        CookieManager.getInstance().flush()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val address = request.url
        when (address.scheme) {
            null, "http", "https" -> return false
        }
        try {
            val destination = if (address.scheme == "intent") {
                Intent.parseUri(address.toString(), Intent.URI_INTENT_SCHEME).apply {
                    component = null
                    selector = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, address)
            }
            view.context.startActivity(destination)
        } catch (_: Exception) {
            // A page can link to an app that is unavailable on this device.
        }
        return true
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        view.destroy()
        rendererLost()
        return true
    }
}
