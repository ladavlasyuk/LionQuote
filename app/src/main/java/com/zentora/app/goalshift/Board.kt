package com.zentora.app.goalshift

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import kotlin.math.*

class Board(
    context: Context,
    private val journey: Journey,
    private val art: Art,
    onStep: (Int) -> Unit,
) : ViewGroup(context) {
    private val trail = journey.trail
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF()
    private var previous = journey.current
    private var progress = 1f
    private var animator: ValueAnimator? = null
    private var side = 0f
    private var leftEdge = 0f
    private var topEdge = 0f

    init {
        setWillNotDraw(false)
        for (cell in trail.tiles.indices) {
            if (trail.tiles[cell] == '#') continue
            val tile = View(context)
            tile.tag = cell
            tile.isFocusable = true
            tile.setOnClickListener { onStep(cell) }
            addView(tile)
        }
        describeTiles()
    }

    private fun describeTiles() {
        val state = journey.current
        for (i in 0 until childCount) {
            val tile = getChildAt(i)
            val cell = tile.tag as Int
            val terrain =
                when (trail.tiles[cell]) {
                    'R' -> R.string.roar_tile
                    's' -> R.string.sun_tile
                    'm' -> R.string.moon_tile
                    'G' -> R.string.gate_tile
                    else ->
                        if (trail.hasShard(cell, state)) R.string.shard_tile else R.string.sand_tile
                }
            val detail = mutableListOf(context.getString(terrain))
            val canMove = trail.step(state, cell) != null
            if (cell == state.cell) detail += context.getString(R.string.here)
            if (!trail.passable(cell, state))
                detail +=
                    context.getString(
                        if (trail.tiles[cell] == 'G') R.string.locked else R.string.lowered
                    )
            if (canMove) detail += context.getString(R.string.move_here)
            tile.isClickable = canMove
            tile.contentDescription =
                context.getString(
                    R.string.cell,
                    1 + cell / trail.size,
                    1 + cell % trail.size,
                    detail.joinToString(". "),
                )
        }
    }

    fun advance(from: Position) {
        animator?.cancel()
        previous = from
        describeTiles()
        animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = if (from.night == journey.current.night) 165 else 310
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        val height = MeasureSpec.getSize(h)
        setMeasuredDimension(width, height)
        side = min(width * .91f / trail.size, height * .88f / trail.size)
        leftEdge = (width - side * trail.size) / 2
        topEdge = (height - side * trail.size) / 2
        val spec = MeasureSpec.makeMeasureSpec(ceil(side).toInt(), MeasureSpec.EXACTLY)
        for (i in 0 until childCount) getChildAt(i).measure(spec, spec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val tile = getChildAt(i)
            val cell = tile.tag as Int
            val x = (leftEdge + side * (cell % trail.size)).roundToInt()
            val y = (topEdge + side * (cell / trail.size)).roundToInt()
            tile.layout(x, y, x + tile.measuredWidth, y + tile.measuredHeight)
        }
    }

    override fun onDraw(c: Canvas) {
        val state = journey.current
        val night = state.night
        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density
        val bg = if (night) color("253846") else color("394B42")
        p.color = bg
        c.drawRoundRect(0f, 0f, w, h, 12 * density, 12 * density, p)
        val spacing = side * .5f
        if (spacing <= 0) return
        p.color = if (night) color("516470") else color("647361")
        var x = leftEdge % spacing
        while (x < w) {
            var y = topEdge % spacing
            while (y < h) {
                c.drawCircle(x, y, density * .65f, p)
                y += spacing
            }
            x += spacing
        }
        for (cell in trail.tiles.indices) {
            val type = trail.tiles[cell]
            if (type == '#') {
                val seed = (cell * 17 + journey.index * 13) % 29
                val px = leftEdge + side * (cell % trail.size + .36f + (seed % 4) * .08f)
                val py = topEdge + side * (cell / trail.size + .4f + (seed % 3) * .1f)
                if (seed % 4 == 0 || seed % 7 == 0) {
                    c.save()
                    c.translate(px, py)
                    c.scale(side / 100f, side / 100f)
                    p.color = if (night) color("30424B") else color("34453B")
                    c.drawOval(-16f, 1f, 18f, 8f, p)
                    p.color = if (night) color("435463") else color("526850")
                    c.drawRect(-8f, -9f, -5f, 3f, p)
                    c.drawRect(-5f, -4f, 1f, 4f, p)
                    c.drawRect(1f, -14f, 5f, 4f, p)
                    c.drawRect(6f, -4f, 10f, 4f, p)
                    c.drawRect(10f, -8f, 13f, -2f, p)
                    p.color = if (night) color("667480") else color("829170")
                    c.drawRect(1f, -14f, 4f, -10f, p)
                    c.restore()
                }
                continue
            }
            val accessible = trail.passable(cell, state)
            val raised = accessible || type == 'G'
            val cx = leftEdge + side * (cell % trail.size + .5f)
            val cy = topEdge + side * (cell / trail.size + .5f)
            c.save()
            c.translate(cx - side / 2, cy - side / 2)
            c.scale(side / 100, side / 100)
            val top = if (raised) 4f else 17f
            val bottom = if (raised) 79f else 89f
            p.color = if (raised) color("725638") else color("314043")
            c.drawRoundRect(3f, top + 9f, 97f, 95f, 5f, 5f, p)
            p.color =
                when {
                    !raised -> color("586668")
                    type == 'R' -> color("E9A74F")
                    type == 'm' -> color("9FB7CB")
                    type == 's' -> color("FFD27D")
                    night -> color("BAAC91")
                    else -> color("E5C68A")
                }
            c.drawRoundRect(3f, top, 97f, bottom, 5f, 5f, p)
            p.color = if (raised) 0x66FFF3BE else 0x228AA7B0
            c.drawRoundRect(6f, top + 2, 94f, top + 5, 2f, 2f, p)
            val mark =
                when (type) {
                    'R' -> "roar"
                    's' -> "sun"
                    'm' -> "moon"
                    'G' -> "gate"
                    else -> ""
                }
            if (mark.isNotEmpty())
                c.glyph(
                    mark,
                    50f,
                    (top + bottom) / 2,
                    if (type == 'G') 57f else 37f,
                    if (raised) color("745432") else color("9DABA6"),
                )
            if (trail.hasShard(cell, state)) {
                p.color = 0x40512D14
                c.drawOval(33f, 59f, 68f, 67f, p)
                c.glyph("shard", 50f, 40f, 52f, color("99501F"))
                c.glyph("shard", 49f, 37f, 42f, color("FFDF55"))
                p.color = Color.WHITE
                c.drawLine(45f, 33f, 49f, 27f, p)
            }
            if (type == 'G' && state.shards != 7) c.glyph("lock", 50f, 55f, 22f, color("614B35"))
            c.restore()
        }
        val prev = previous.cell
        val cur = state.cell
        val cx =
            leftEdge +
                side * ((prev % trail.size) * (1 - progress) + (cur % trail.size) * progress + .5f)
        val cy =
            topEdge +
                side * ((prev / trail.size) * (1 - progress) + (cur / trail.size) * progress + .40f)
        val bounce = if (prev != cur) sin(PI * progress).toFloat() * side * .18f else 0f
        p.color = 0x55483321
        c.drawOval(cx - side * .34f, cy + side * .2f, cx + side * .34f, cy + side * .39f, p)
        if (previous.night != night && progress < 1f) {
            p.color = gold
            p.alpha = (195 * (1 - progress)).toInt()
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2 * density
            c.drawCircle(cx, cy, side * (.3f + progress), p)
            p.style = Paint.Style.FILL
        }
        bounds.set(
            cx - side * .44f,
            cy - side * .46f - bounce,
            cx + side * .44f,
            cy + side * .40f - bounce,
        )
        p.color = Color.WHITE
        c.drawBitmap(art.lion, art.face, bounds, p)
    }
}
