package com.zentora.app.goalshift

import android.app.AlertDialog
import android.content.Intent
import com.zentora.app.goalshift.passage.PolicyReaderActivity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*

private enum class Screen {
    HOME,
    MAP,
    GAME,
    RESULT,
    DEN,
}

class MainActivity : ComponentActivity() {
    private lateinit var art: Art
    private lateinit var frame: FrameLayout
    private val prefs by lazy { getSharedPreferences("trails", Context.MODE_PRIVATE) }
    private val names by lazy { resources.getStringArray(R.array.names) }
    private val quotes by lazy { resources.getStringArray(R.array.quotes) }
    private var screen = Screen.HOME
    private var journey = Journey(0)
    private var popup: AlertDialog? = null
    private var openedQuote = -1
    private var paused = false
    private lateinit var board: Board
    private lateinit var phase: TextView
    private lateinit var shards: TextView
    private lateinit var moves: TextView
    private lateinit var hint: TextView
    private lateinit var undo: View
    private lateinit var roar: View
    private val full = -1
    private val wrap = -2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        art = Art(this)
        frame = FrameLayout(this).apply { setBackgroundColor(ink) }
        ViewCompat.setOnApplyWindowInsetsListener(frame) { v, insets ->
            val safe =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        setContentView(frame)
        journey = Journey.decode(prefs.getString("active", null)) ?: Journey(0)
        if (savedInstanceState != null) {
            screen = Screen.valueOf(savedInstanceState.getString("screen") ?: "HOME")
            if (screen == Screen.RESULT)
                journey = Journey.decode(prefs.getString("last", null)) ?: journey
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = back()
            },
        )
        render()
        if (savedInstanceState?.getBoolean("paused") == true && screen == Screen.GAME) pause()
        val card = savedInstanceState?.getInt("quote", -1) ?: -1
        if (card in gardens.indices && screen == Screen.DEN) quoteDialog(card)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("screen", screen.name)
        outState.putBoolean("paused", paused)
        outState.putInt("quote", openedQuote)
    }

    private fun best(i: Int) = prefs.getInt("best_$i", 0)

    private fun count() = gardens.indices.count { best(it) > 0 }

    private fun nextIndex() = gardens.indices.firstOrNull { best(it) == 0 } ?: 0

    private fun save() {
        prefs.edit().putString("active", journey.encode()).apply()
    }

    private fun start(index: Int, resume: Boolean = false) {
        journey =
            if (resume)
                Journey.decode(prefs.getString("active", null))?.takeIf { it.index == index }
                    ?: Journey(index)
            else Journey(index)
        save()
        go(Screen.GAME)
    }

    private fun go(next: Screen) {
        popup?.dismiss()
        screen = next
        render()
    }

    private fun back() {
        when (screen) {
            Screen.GAME -> pause()
            Screen.RESULT -> go(Screen.MAP)
            Screen.MAP,
            Screen.DEN -> go(Screen.HOME)
            Screen.HOME -> Unit
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).roundToInt()

    private fun shape(fill: Int, border: Int = fill, radius: Int = 10) =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            setStroke(dp(1), border)
        }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun row() = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

    private fun View.pad(h: Int, v: Int) = setPadding(dp(h), dp(v), dp(h), dp(v))

    private fun LinearLayout.put(v: View, h: Int = wrap, w: Int = full, weight: Float = 0f) {
        addView(
            v,
            LinearLayout.LayoutParams(if (w < 0) w else dp(w), if (h < 0) h else dp(h), weight),
        )
    }

    private fun LinearLayout.gap(n: Int) = put(Space(this@MainActivity), n, n)

    private fun label(
        s: String,
        size: Int,
        tint: Int = cream,
        face: String = "sans-serif",
    ): TextView =
        TextView(this).apply {
            text = s
            textSize = size.toFloat()
            setTextColor(tint)
            typeface = Typeface.create(face, Typeface.NORMAL)
            includeFontPadding = false
        }

    private fun tag(s: String, tint: Int = mint) =
        label(s, 11, tint, "sans-serif-medium").apply { letterSpacing = .13f }

    private fun title(s: String, size: Int = 38) =
        label(s, size, cream, "serif").apply { typeface = Typeface.create("serif", Typeface.BOLD) }

    private fun icon(name: String, tint: Int, size: Int) =
        object : View(this) {
            override fun onDraw(c: Canvas) {
                c.glyph(name, width / 2f, height / 2f, dp(size).toFloat(), tint)
            }
        }

    private fun clickable(view: View, fill: Int, border: Int, action: () -> Unit) {
        view.background =
            RippleDrawable(ColorStateList.valueOf(0x335E8778), shape(fill, border), null)
        view.isFocusable = true
        view.setOnClickListener { action() }
    }

    private fun iconButton(name: String, res: Int, action: () -> Unit) =
        icon(name, cream, 20).apply {
            contentDescription = getString(res)
            clickable(this, panel, line, action)
        }

    private fun button(
        res: Int,
        symbol: String,
        primary: Boolean = false,
        action: () -> Unit,
    ): LinearLayout {
        val tint = if (primary) ink else cream
        return row().apply {
            minimumHeight = dp(52)
            pad(16, 8)
            gravity = Gravity.CENTER
            clickable(this, if (primary) gold else panel, if (primary) gold else line, action)
            put(icon(symbol, tint, 20), 24, 24)
            gap(8)
            put(label(getString(res), 16, tint, "sans-serif-medium"), wrap, wrap)
            contentDescription = getString(res)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            for (i in 0 until childCount) getChildAt(i).importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun render() {
        frame.removeAllViews()
        val container = column()
        frame.addView(container, FrameLayout.LayoutParams(full, full, Gravity.CENTER))
        val header = row().apply { pad(20, 6) }
        container.put(header, 60)
        if (screen == Screen.HOME) {
            header.put(icon("brand", gold, 29), 36, 32)
            header.gap(8)
            header.put(label(getString(R.string.app_name), 22, cream, "serif"), wrap, 0, 1f)
            header.put(iconButton("book", R.string.den) { go(Screen.DEN) }, 48, 48)
        } else {
            header.put(iconButton("back", R.string.back) { back() }, 48, 48)
            header.gap(12)
            header.put(
                tag(getString(R.string.campaign).uppercase(java.util.Locale.ENGLISH)),
                wrap,
                0,
                1f,
            )
            header.put(icon("brand", gold, 24), 32, 32)
        }
        container.put(View(this).apply { setBackgroundColor(line) }, 1)
        if (screen == Screen.GAME) {
            game(container)
            return
        }
        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = false
            }
        container.put(scroll, 0, full, 1f)
        val page = column().apply { pad(20, 20) }
        val contentWidth = min(resources.configuration.screenWidthDp - 40, 640).coerceAtLeast(160)
        val center = FrameLayout(this)
        scroll.addView(center)
        center.addView(
            page,
            FrameLayout.LayoutParams(
                dp(contentWidth + 40),
                wrap,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ),
        )
        when (screen) {
            Screen.HOME -> {
                home(page)
                container.put(button(R.string.lion_policy, "book") {
                    startActivity(Intent(this, PolicyReaderActivity::class.java))
                }, 52)
            }
            Screen.MAP -> catalogue(page, false)
            Screen.DEN -> catalogue(page, true)
            Screen.RESULT -> result(page)
            else -> Unit
        }
    }

    private fun home(page: LinearLayout) {
        val hero = FrameLayout(this)
        val h = (resources.configuration.screenWidthDp * .72f).roundToInt().coerceIn(234, 360)
        val largeType =
            resources.configuration.fontScale > 1.12f || resources.configuration.screenWidthDp < 340
        val mast = column()
        if (largeType) {
            page.put(mast)
            mast.put(tag(getString(R.string.gardens_label), gold))
            mast.gap(6)
            mast.put(title(getString(R.string.campaign), 30))
            page.gap(12)
        }
        page.put(hero, if (largeType) 164 else h)
        hero.addView(Scenery(this, art), FrameLayout.LayoutParams(full, full))
        if (!largeType) {
            mast.pad(16, 16)
            hero.addView(mast, FrameLayout.LayoutParams(full, wrap))
            mast.put(tag(getString(R.string.gardens_label), ink))
            mast.gap(8)
            mast.put(
                title(getString(R.string.hero_title), 31).apply {
                    setTextColor(ink)
                    setLineSpacing(-dp(3).toFloat(), 1f)
                }
            )
        }
        page.gap(22)
        val active = Journey.decode(prefs.getString("active", null))
        val index = active?.index ?: nextIndex()
        val progress =
            column().apply {
                pad(16, 16)
                background = shape(panel, line)
            }
        page.put(progress)
        val top = row()
        progress.put(top)
        top.put(tag(getString(R.string.journey_label), gold), wrap, 0, 1f)
        top.put(
            label(getString(R.string.number, count()) + " / 12", 12, mint, "monospace"),
            wrap,
            wrap,
        )
        progress.gap(10)
        progress.put(title(names[index], 26))
        progress.gap(12)
        val segments = row()
        progress.put(segments, 5)
        for (i in gardens.indices) {
            if (i > 0) segments.gap(3)
            segments.put(
                View(this).apply {
                    background = shape(if (best(i) > 0) gold else line, radius = 2)
                },
                5,
                0,
                1f,
            )
        }
        progress.gap(14)
        progress.put(
            button(if (active != null) R.string.resume_run else R.string.play, "play", true) {
                start(index, active != null)
            }
        )
        page.gap(12)
        val links = row()
        page.put(links)
        links.put(button(R.string.map, "map") { go(Screen.MAP) }, wrap, 0, 1f)
        links.gap(10)
        links.put(button(R.string.den, "book") { go(Screen.DEN) }, wrap, 0, 1f)
    }

    private fun catalogue(page: LinearLayout, den: Boolean) {
        page.put(tag(getString(if (den) R.string.collection else R.string.gardens_label), gold))
        page.gap(6)
        page.put(title(getString(if (den) R.string.den else R.string.map)))
        page.gap(6)
        page.put(
            label(
                getString(if (den) R.string.quote_count else R.string.complete_count, count()),
                15,
                mint,
            )
        )
        page.gap(18)
        for (i in gardens.indices) {
            val earned = best(i) > 0
            val open = if (den) earned else earned || i == nextIndex()
            val item =
                row().apply {
                    minimumHeight = dp(75)
                    pad(12, 10)
                }
            val action = { if (den) quoteDialog(i) else start(i, true) }
            if (open)
                clickable(item, panel, if (!den && i == nextIndex()) darkGold else line, action)
            else item.background = shape(ink, line)
            val badge =
                FrameLayout(this).apply {
                    background =
                        shape(
                            if (earned) gold else if (open) color("514530") else panel,
                            radius = 8,
                        )
                }
            val marker =
                if (open)
                    label(
                            getString(R.string.number, i + 1),
                            16,
                            if (earned) ink else gold,
                            "monospace",
                        )
                        .apply { gravity = Gravity.CENTER }
                else icon("lock", mint, 16)
            badge.addView(marker, FrameLayout.LayoutParams(full, full))
            item.put(badge, 42, 42)
            item.gap(12)
            val words = column()
            item.put(words, wrap, 0, 1f)
            words.put(label(names[i], 19, if (open) cream else mint, "serif"))
            words.gap(4)
            val sub =
                when {
                    den && earned -> getString(R.string.quote_format, quotes[i])
                    den -> getString(R.string.quote_locked)
                    earned -> getString(R.string.best, best(i))
                    open -> getString(R.string.available)
                    else -> getString(R.string.locked)
                }
            words.put(label(sub, 13, mint))
            if (open) item.put(icon("chevron", gold, 17), 24, 24)
            page.put(item)
            if (i < gardens.lastIndex) {
                val link = FrameLayout(this)
                page.put(link, 8)
                if (!den)
                    link.addView(
                        View(this).apply { setBackgroundColor(if (earned) darkGold else line) },
                        FrameLayout.LayoutParams(dp(2), full).apply { leftMargin = dp(32) },
                    )
            }
        }
    }

    private fun game(page: LinearLayout) {
        val wide = resources.configuration.screenWidthDp > resources.configuration.screenHeightDp
        val layout =
            LinearLayout(this).apply {
                orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                pad(14, 12)
            }
        page.put(layout, 0, full, 1f)
        board = Board(this, journey, art) { act(it) }
        val info = column()
        fun heading(parent: LinearLayout) {
            parent.put(tag(getString(R.string.trail_no, journey.index + 1), gold))
            parent.gap(4)
            parent.put(title(names[journey.index], if (wide) 24 else 30))
            parent.gap(8)
            val stats = row()
            parent.put(stats)
            phase = label("", 14, gold, "sans-serif-medium")
            shards = label("", 14, cream)
            moves = label("", 14, mint)
            for (v in listOf(phase, shards, moves)) {
                stats.put(v, wrap, wrap)
                stats.gap(12)
            }
        }
        if (wide) {
            layout.put(board, full, 0, 1.2f)
            layout.gap(14)
            val scroll = ScrollView(this)
            layout.put(scroll, full, 0, 1f)
            scroll.addView(info)
            heading(info)
        } else {
            heading(layout)
            layout.gap(10)
            layout.put(board, 0, full, 1f)
            layout.put(info)
        }
        info.gap(8)
        hint = label("", 14, mint).apply { minLines = 2 }
        info.put(hint)
        info.gap(10)
        val controls = row()
        info.put(controls)
        undo = button(R.string.undo, "undo") { act(null) }
        controls.put(undo, wrap, 0, 1f)
        controls.gap(8)
        roar = button(R.string.roar, "roar", true) { act(Trail.ROAR) }
        controls.put(roar, wrap, 0, 1f)
        controls.gap(8)
        controls.put(iconButton("pause", R.string.pause) { pause() }, 52, 48)
        syncHud()
    }

    private fun syncHud() {
        val state = journey.current
        val canRoar = journey.trail.tiles[state.cell] == 'R'
        phase.text =
            (if (state.night) "☾  " else "☀  ") +
                getString(if (state.night) R.string.night else R.string.day)
        shards.text = getString(R.string.shards, Integer.bitCount(state.shards))
        moves.text =
            if (journey.moves == 1) getString(R.string.move_one)
            else getString(R.string.moves, journey.moves)
        hint.setText(
            when {
                state.shards == 7 -> R.string.gate_hint
                canRoar -> R.string.roar_hint
                journey.moves == 0 && journey.index == 0 -> R.string.first_hint
                else -> R.string.trail_hint
            }
        )
        undo.isEnabled = journey.moves > 0
        undo.alpha = if (undo.isEnabled) 1f else .45f
        roar.isEnabled = canRoar
        roar.alpha = if (canRoar) 1f else .44f
    }

    private fun act(target: Int?) {
        val prev = journey.current
        if (!(if (target == null) journey.undo() else journey.act(target))) return
        if (journey.complete) {
            val old = best(journey.index)
            prefs
                .edit()
                .remove("active")
                .putInt(
                    "best_${journey.index}",
                    if (old == 0) journey.moves else min(old, journey.moves),
                )
                .putString("last", journey.encode())
                .apply()
            go(Screen.RESULT)
        } else {
            save()
            board.advance(prev)
            syncHud()
        }
    }

    private fun result(page: LinearLayout) {
        val i = journey.index
        page.put(tag(getString(R.string.trail_no, i + 1), gold))
        page.gap(6)
        page.put(title(getString(R.string.complete)))
        page.gap(12)
        page.put(Scenery(this, art, i), 170)
        page.gap(16)
        page.put(title(names[i], 25))
        page.gap(6)
        page.put(
            label(
                getString(R.string.moves, journey.moves) +
                    "   ·   " +
                    getString(R.string.best, best(i)),
                14,
                mint,
            )
        )
        page.gap(14)
        page.put(quotePanel(i))
        page.gap(14)
        page.put(
            button(
                if (i == gardens.lastIndex) R.string.open_den else R.string.next,
                "arrow",
                true,
            ) {
                if (i == gardens.lastIndex) go(Screen.DEN) else start(i + 1)
            }
        )
        page.gap(8)
        page.put(button(R.string.replay, "undo") { start(i) })
    }

    private fun quotePanel(i: Int) =
        column().apply {
            background = shape(color("F1DDAE"), radius = 10)
            pad(18, 16)
            put(tag(getString(R.string.quote_unlocked), color("694D30")))
            gap(10)
            put(label(getString(R.string.quote_format, quotes[i]), 23, ink, "serif"))
        }

    private fun showPanel(content: LinearLayout) {
        val scroll = ScrollView(this).apply { addView(content) }
        val dialog = AlertDialog.Builder(this).setView(scroll).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnDismissListener {
            paused = false
            openedQuote = -1
            popup = null
        }
        popup = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun pause() {
        if (popup != null) return
        paused = true
        val box =
            column().apply {
                background = shape(panel, line, 14)
                pad(20, 20)
            }
        box.put(tag(getString(R.string.trail_no, journey.index + 1), gold))
        box.gap(8)
        box.put(title(getString(R.string.paused), 30))
        box.gap(18)
        box.put(button(R.string.resume, "play", true) { popup?.dismiss() })
        box.gap(8)
        box.put(button(R.string.restart, "undo") { start(journey.index) })
        box.gap(8)
        box.put(button(R.string.map, "map") { go(Screen.MAP) })
        showPanel(box)
    }

    private fun quoteDialog(i: Int) {
        openedQuote = i
        val box =
            column().apply {
                background = shape(panel, line, 14)
                pad(16, 16)
            }
        box.put(title(names[i], 25))
        box.gap(12)
        box.put(Scenery(this, art, i), 125)
        box.gap(10)
        box.put(quotePanel(i))
        box.gap(12)
        box.put(button(R.string.close, "back") { popup?.dismiss() })
        showPanel(box)
    }
}
