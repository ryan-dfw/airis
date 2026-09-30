package com.tollview.airis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * The AIris mark: ten curved blades spiralling into an iris around a glowing pupil.
 * Same geometry as art/airis-logo.svg and the launcher icon.
 */
private const val BLADES = 10
private const val SWEEP_DEG = 150f
private const val INNER = 0.34f
private const val WIDTH = 0.13f

val MarkViolet = Color(0xFF8B7CFF)
val MarkRose = Color(0xFFE0608A)
val MarkAmber = Color(0xFFF7A15A)

/** Blade outlines in a unit circle centred on the origin. */
private fun markPath(): Path {
    val path = Path()
    val steps = 24
    for (i in 0 until BLADES) {
        val a0 = 360f / BLADES * i
        val left = ArrayList<Offset>(steps + 1)
        val right = ArrayList<Offset>(steps + 1)
        for (k in 0..steps) {
            val t = k / steps.toFloat()
            fun at(tt: Float): Offset {
                val r = 1f - (1f - INNER) * tt
                val ang = Math.toRadians((a0 + SWEEP_DEG * tt).toDouble())
                return Offset((r * cos(ang)).toFloat(), (r * sin(ang)).toFloat())
            }
            val p = at(t)
            val q = at(t + 0.001f)
            val dx = q.x - p.x
            val dy = q.y - p.y
            val len = hypot(dx, dy)
            val nx = -dy / len
            val ny = dx / len
            val w = WIDTH * sin(PI * t).toFloat().pow(0.9f) * (1f - 0.35f * t)
            left += Offset(p.x + nx * w / 2, p.y + ny * w / 2)
            right += Offset(p.x - nx * w / 2, p.y - ny * w / 2)
        }
        val outline = left + right.asReversed()
        path.moveTo(outline[0].x, outline[0].y)
        for (j in 1 until outline.size) path.lineTo(outline[j].x, outline[j].y)
        path.close()
    }
    return path
}

/**
 * Draws the mark. With [alive], the iris turns very slowly and the pupil breathes.
 * The mark fills about 80% of the given size; the rest is its glow.
 */
@Composable
fun AirisMark(modifier: Modifier = Modifier, alive: Boolean = true, alpha: Float = 1f) {
    val path = remember { markPath() }
    val transition = rememberInfiniteTransition(label = "mark")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_800), RepeatMode.Reverse),
        label = "breathe",
    )
    val spin = if (alive) turn else 0f
    val pulse = if (alive) breathe else 0.5f

    Canvas(modifier) {
        val c = center
        val s = size.minDimension * 0.40f

        // Soft glow behind.
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFFF08A4B).copy(alpha = (0.45f + 0.15f * pulse) * alpha),
                0.5f to Color(0xFF6C5CE0).copy(alpha = 0.18f * alpha),
                1f to Color.Transparent,
                center = c,
                radius = s * 1.35f,
            ),
            radius = s * 1.35f,
        )

        // Blades.
        rotate(spin, pivot = c) {
            translate(c.x, c.y) {
                scale(s, s, pivot = Offset.Zero) {
                    drawPath(
                        path,
                        Brush.linearGradient(
                            0f to MarkViolet,
                            0.5f to MarkRose,
                            1f to MarkAmber,
                            start = Offset(-1f, -1f),
                            end = Offset(1f, 1f),
                        ),
                        alpha = 0.92f * alpha,
                    )
                }
            }
        }

        // Pupil.
        val pr = s * (0.20f + 0.04f * pulse)
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFFFFF6EA).copy(alpha = alpha),
                0.45f to Color(0xFFFFD2A6).copy(alpha = 0.9f * alpha),
                1f to Color(0x00F08A4B),
                center = c,
                radius = pr,
            ),
            radius = pr,
        )
    }
}
