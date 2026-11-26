package com.zentora.app.goalshift

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

fun color(value: String) = Color.parseColor("#$value")

val ink = color("142424")
val panel = color("203333")
val line = color("40514B")
val cream = color("FFF0CB")
val mint = color("B2C1AA")
val gold = color("FFCB62")
val darkGold = color("B47B34")

class Art(context: Context) {
    val lion: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.garden_lion)
    val face = Rect(106, 0, 798, 674)
}

private fun Canvas.shape(p: Paint, shade: Int, vararg coords: Float) {
    p.color = shade
    val path = Path()
    path.moveTo(coords[0], coords[1])
    for (i in 2 until coords.size step 2) path.lineTo(coords[i], coords[i + 1])
    path.close()
    drawPath(path, p)
}

fun Canvas.glyph(name: String, cx: Float, cy: Float, size: Float, tint: Int) {
    save()
    translate(cx - size / 2, cy - size / 2)
    scale(size / 24, size / 24)
    val p =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    fun stroke(vararg v: Float) {
        val path =
            Path().apply {
                moveTo(v[0], v[1])
                for (i in 2 until v.size step 2) lineTo(v[i], v[i + 1])
            }
        p.style = Paint.Style.STROKE
        drawPath(path, p)
        p.style = Paint.Style.FILL
    }
    when (name) {
        "sun",
        "brand" -> {
            for (i in 0..7) {
                save()
                rotate(45f * i, 12f, 12f)
                drawLine(12f, 1.5f, 12f, 4f, p)
                restore()
            }
            if (name == "brand") shape(p, tint, 12f, 5.6f, 18.4f, 12f, 12f, 18.4f, 5.6f, 12f)
            else drawCircle(12f, 12f, 4.7f, p)
        }
        "moon" -> {
            val path =
                Path().apply {
                    moveTo(15f, 2f)
                    cubicTo(2f, 0f, -1f, 18f, 11f, 21f)
                    quadTo(18f, 23f, 22f, 15f)
                    cubicTo(12f, 18f, 8f, 9f, 15f, 2f)
                    close()
                }
            drawPath(path, p)
        }
        "shard" -> {
            shape(p, tint, 12f, 1f, 20f, 10f, 12f, 23f, 4f, 10f)
            stroke(12f, 4f, 10f, 10f)
        }
        "arrow" -> {
            stroke(4f, 12f, 20f, 12f, 14f, 6f)
            stroke(20f, 12f, 14f, 18f)
        }
        "back" -> {
            stroke(20f, 12f, 4f, 12f, 10f, 6f)
            stroke(4f, 12f, 10f, 18f)
        }
        "chevron" -> stroke(9f, 6f, 15f, 12f, 9f, 18f)
        "pause" -> {
            drawRect(6f, 4f, 10f, 20f, p)
            drawRect(14f, 4f, 18f, 20f, p)
        }
        "map" -> {
            stroke(2f, 5f, 9f, 2f, 16f, 6f, 22f, 3f, 22f, 19f, 16f, 22f, 9f, 18f, 2f, 21f, 2f, 5f)
            stroke(9f, 3f, 9f, 17f)
            stroke(16f, 7f, 16f, 21f)
        }
        "book" -> {
            stroke(
                3f,
                4f,
                9f,
                4f,
                12f,
                6f,
                15f,
                4f,
                21f,
                4f,
                21f,
                19f,
                15f,
                19f,
                12f,
                21f,
                9f,
                19f,
                3f,
                19f,
                3f,
                4f,
            )
            stroke(12f, 7f, 12f, 20f)
        }
        "lock" -> {
            p.style = Paint.Style.STROKE
            drawRoundRect(8f, 3f, 16f, 15f, 4f, 4f, p)
            p.style = Paint.Style.FILL
            drawRoundRect(5f, 10f, 19f, 21f, 2f, 2f, p)
        }
        "undo" -> {
            stroke(8f, 4f, 3f, 9f, 8f, 14f)
            stroke(4f, 9f, 15f, 9f, 20f, 13f, 20f, 17f, 16f, 20f, 10f, 20f)
        }
        "roar" -> {
            drawOval(6f, 11f, 18f, 21f, p)
            for (i in 0..3) drawOval(
                2f + 5 * i,
                if (i in 1..2) 2f else 5f,
                6f + 5 * i,
                if (i in 1..2) 9f else 12f,
                p,
            )
        }
        "gate" -> {
            stroke(3f, 21f, 3f, 8f, 7f, 3f, 17f, 3f, 21f, 8f, 21f, 21f)
            stroke(7f, 21f, 7f, 10f, 10f, 7f, 14f, 7f, 17f, 10f, 17f, 21f)
        }
        "check" -> stroke(3f, 12f, 9f, 18f, 21f, 5f)
        "play" -> shape(p, tint, 7f, 3f, 21f, 12f, 7f, 21f)
    }
    restore()
}

class Scenery(context: Context, val art: Art, val chapter: Int = 0, val portrait: Boolean = true) :
    View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val theme = chapter % 3
        val sky =
            arrayOf(
                intArrayOf(color("769989"), color("D7CD8D"), color("EAB66C")),
                intArrayOf(color("434E62"), color("BC9B82"), color("E8BD78")),
                intArrayOf(color("314E59"), color("809F96"), color("D7C78E")),
            )[theme]
        c.save()
        c.clipPath(
            Path().apply {
                addRoundRect(
                    0f,
                    0f,
                    w,
                    h,
                    context.resources.displayMetrics.density * 12,
                    context.resources.displayMetrics.density * 12,
                    Path.Direction.CW,
                )
            }
        )
        p.shader = LinearGradient(0f, 0f, 0f, h, sky, null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        c.scale(w / 400, h / 255)
        val sunX = if (portrait) 294f else 212f
        p.color = color("F9DF9D")
        c.drawCircle(sunX, 75f, 34f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.9f
        p.alpha = 140
        c.drawCircle(sunX, 75f, 40f, p)
        p.style = Paint.Style.FILL
        p.alpha = 255
        fun ridge(y: Float, seed: Int, col: Int, amp: Int) {
            val path =
                Path().apply {
                    moveTo(-10f, 265f)
                    lineTo(-10f, y)
                }
            val r = java.util.Random(seed.toLong())
            var x = -10f
            while (x < 410) {
                val yy = y - r.nextInt(amp)
                path.lineTo(x, yy)
                x += 8 + r.nextInt(22)
                path.lineTo(x, yy)
            }
            path.lineTo(x, 265f)
            path.close()
            p.color = col
            c.drawPath(path, p)
        }
        ridge(165f, 19 + chapter, color("A0A47E"), 25)
        ridge(185f, 128 + chapter, color("788A70"), 29)
        ridge(211f, 909 + chapter, color("536F60"), 24)
        fun tree(x: Float, y: Float, s: Float) {
            c.save()
            c.translate(x, y)
            c.scale(s, s)
            p.color = color("334E45")
            c.drawRect(-2f, -29f, 3f, 0f, p)
            c.drawRect(-10f, -35f, -6f, -22f, p)
            c.drawRect(-8f, -25f, 0f, -21f, p)
            c.drawRect(-22f, -40f, 13f, -32f, p)
            c.drawRect(-15f, -45f, 8f, -38f, p)
            c.drawRect(1f, -36f, 27f, -29f, p)
            c.drawRect(7f, -40f, 21f, -34f, p)
            c.restore()
        }
        tree(28f, 199f, 0.78f)
        tree(347f, 179f, .48f)
        tree(385f, 190f, .65f)
        ridge(241f, 55, color("3F6152"), 14)
        c.shape(p, color("E2B464"), 141f, 225f, 342f, 188f, 419f, 210f, 419f, 260f, 230f, 260f)
        c.shape(p, color("B88040"), 141f, 225f, 230f, 260f, 140f, 260f)
        c.shape(p, color("F9D68A"), 195f, 215f, 299f, 195f, 350f, 210f, 247f, 231f)
        c.shape(p, color("A56E38"), 195f, 215f, 247f, 231f, 247f, 240f, 195f, 224f)
        c.shape(p, color("C39148"), 247f, 231f, 350f, 210f, 350f, 219f, 247f, 240f)
        c.restore()
        if (portrait) {
            val sh = min(w * .51f, h * .79f)
            val x = w * .69f
            val bottom = h * .86f
            p.color = 0x44372412
            c.drawOval(x - sh * .31f, bottom - sh * .05f, x + sh * .37f, bottom + sh * .03f, p)
            p.color = Color.WHITE
            c.drawBitmap(art.lion, null, RectF(x - sh / 2, bottom - sh, x + sh / 2, bottom), p)
        }
    }
}
