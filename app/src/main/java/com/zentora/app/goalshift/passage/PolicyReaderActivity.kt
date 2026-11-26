package com.zentora.app.goalshift.passage

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity

class PolicyReaderActivity : ComponentActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
        }
        val back = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            contentDescription = "Back"
            setOnClickListener { finish() }
        }
        val buttonSize = (48 * resources.displayMetrics.density).toInt()
        root.addView(back, buttonSize, buttonSize)

        val content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        val loading = ProgressBar(this)
        webView = WebView(this).apply {
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    loading.visibility = View.GONE
                }
            }
        }
        content.addView(webView)
        content.addView(loading, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        setContentView(root)
        webView.loadUrl(RouteClient.PRIVACY_POLICY_URL)
    }

    override fun onDestroy() {
        (webView.parent as FrameLayout).removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
