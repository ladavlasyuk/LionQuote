package com.zentora.app.goalshift.passage

import android.Manifest
import android.content.pm.PackageManager
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.zentora.app.goalshift.R

class ReadingRoomActivity : ComponentActivity() {

    companion object {
        const val PAGE_ADDRESS = "lionquote.reading.address"
        private const val FILE_GATEWAY_CODE = 4829
        private const val DOWNLOAD_PERMISSION_CODE = 4830
    }

    private lateinit var pageView: WebView
    private lateinit var progressWheel: ProgressBar
    private var fileResult: ValueCallback<Array<Uri>>? = null
    private data class PendingDownload(val href: String, val agent: String, val disposition: String, val mime: String)
    private var pendingTransfer: PendingDownload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        val initialUrl = intent.getStringExtra(PAGE_ADDRESS) ?: run {
            finish()
            return
        }

        createSurface()
        configurePage()
        pageView.loadUrl(initialUrl)
        handleBack()
    }

    private fun createSurface() {
        val windowColor = ContextCompat.getColor(this, R.color.reading_window)
        val pageColor = ContextCompat.getColor(this, R.color.reading_paper)
        val indicatorColor = ContextCompat.getColor(this, R.color.reading_progress_tint)

        val container = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(windowColor)
        }

        pageView = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            id = View.generateViewId()
            setBackgroundColor(pageColor)
        }

        progressWheel = ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            indeterminateTintList = ColorStateList.valueOf(indicatorColor)
            visibility = View.VISIBLE
        }

        container.addView(pageView)
        container.addView(progressWheel)
        setContentView(container)

        applyImmersiveChrome()
    }

    private fun configurePage() {
        pageView.applyReadingPreferences()

        pageView.setDownloadListener { href, agent, disposition, mime, _ ->
            requestDownload(href, agent, disposition, mime)
        }

        pageView.webViewClient = ReadingNavigation(
            firstLoadFinished = { progressWheel.visibility = View.GONE },
            rendererLost = { if (!isFinishing && !isDestroyed) recreate() }
        )
        pageView.webChromeClient = fileChooserClient()
    }

    private fun fileChooserClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileResult?.onReceiveValue(null)
                fileResult = callback

                val acceptedFormats = params?.acceptTypes ?: arrayOf("*/*")
                val mimeType = acceptedFormats.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "*/*"

                val documentPicker = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mimeType
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(documentPicker, FILE_GATEWAY_CODE)
                    true
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this@ReadingRoomActivity, "File manager not found", Toast.LENGTH_SHORT).show()
                    callback?.onReceiveValue(null)
                    fileResult = null
                    false
                }
            }
        }
    }

    private fun handleBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pageView.canGoBack()) {
                    pageView.goBack()
                }
            }
        })
    }

    private fun requestDownload(href: String, agent: String, disposition: String, mime: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingTransfer = PendingDownload(href, agent, disposition, mime)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_PERMISSION_CODE)
            return
        }
        enqueueDownload(href, agent, disposition, mime)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == DOWNLOAD_PERMISSION_CODE) {
            val pending = pendingTransfer
            pendingTransfer = null
            if (pending != null && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                enqueueDownload(pending.href, pending.agent, pending.disposition, pending.mime)
            } else {
                Toast.makeText(this, "Storage permission is needed to save this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enqueueDownload(
        downloadHref: String,
        agent: String,
        disposition: String,
        mimeType: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadHref))
            request.setMimeType(mimeType)

            val cookies = CookieManager.getInstance().getCookie(downloadHref)
            if (!cookies.isNullOrEmpty()) {
                request.addRequestHeader("Cookie", cookies)
            }
            request.addRequestHeader("User-Agent", agent)

            val fileName = URLUtil.guessFileName(downloadHref, disposition, mimeType)
            request.setTitle(fileName)
            request.setDescription("Downloading file...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Download started: $fileName", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                android.util.Log.e("LionQuotePage", "Download request failed", error)
            }
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveChrome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_GATEWAY_CODE) {
            absorbPickerResult(resultCode, data)
        }
    }

    private fun absorbPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val selectedFiles = mutableListOf<Uri>()

            data?.data?.let { uri -> selectedFiles.add(uri) }

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> selectedFiles.add(uri) }
                }
            }

            fileResult?.onReceiveValue(selectedFiles.toTypedArray())
        } else {
            fileResult?.onReceiveValue(null)
        }

        fileResult = null
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
