package com.zentora.app.goalshift.passage

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.zentora.app.goalshift.MainActivity
import com.zentora.app.goalshift.R
import kotlinx.coroutines.launch

class ArrivalActivity : ComponentActivity() {
    private val routes by lazy { getSharedPreferences("lion_arrival", MODE_PRIVATE) }
    private lateinit var panel: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        setContentView(panel)
        resolveRoute()
    }

    private fun resolveRoute() {
        panel.removeAllViews()
        panel.addView(ProgressBar(this))
        lifecycleScope.launch {
            when (val route = chooseRoute(routes) { RouteClient(applicationContext).loadDecision() }) {
                ArrivalRoute.Retry -> showRetry()
                ArrivalRoute.Game -> {
                    startActivity(Intent(this@ArrivalActivity, MainActivity::class.java))
                    finish()
                }
                is ArrivalRoute.Page -> openPage(route.address)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun openPage(address: String) {
        startActivity(Intent(this, ReadingRoomActivity::class.java)
            .putExtra(ReadingRoomActivity.PAGE_ADDRESS, address))
        finish()
        overridePendingTransition(R.anim.reading_fade_in, R.anim.reading_fade_out)
    }

    private fun showRetry() {
        panel.removeAllViews()
        panel.addView(TextView(this).apply { setText(R.string.lion_offline) })
        panel.addView(Button(this).apply {
            setText(R.string.lion_retry)
            setOnClickListener { resolveRoute() }
        })
    }
}
