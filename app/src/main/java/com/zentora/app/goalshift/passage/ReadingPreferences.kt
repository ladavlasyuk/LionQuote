package com.zentora.app.goalshift.passage

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
internal fun WebView.applyReadingPreferences() {
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@applyReadingPreferences, true)
    }

    apply {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadWithOverviewMode = false
        useWideViewPort = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        allowFileAccess = true
        allowContentAccess = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        javaScriptCanOpenWindowsAutomatically = true
        cacheMode = WebSettings.LOAD_NO_CACHE
        mediaPlaybackRequiresUserGesture = false
        loadsImagesAutomatically = true
        blockNetworkImage = false
        setSupportMultipleWindows(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = false
        }
        userAgentString = userAgentString.replace("; wv", "").replace("Version/4.0", "")
    }
}
