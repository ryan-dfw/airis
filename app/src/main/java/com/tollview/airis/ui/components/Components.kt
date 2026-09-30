package com.tollview.airis.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tollview.airis.ui.glass.Backdrop
import com.tollview.airis.ui.glass.Glass
import com.tollview.airis.ui.glass.LocalBackdrop
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type

/** Click without a Material ripple: the element gently squishes instead. */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

enum class ButtonKind { Glass, Solid, Quiet }

/** Capsule button. Glass by default; Solid is the bright primary action. */
@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Glass,
    enabled: Boolean = true,
    backdrop: Backdrop? = LocalBackdrop.current,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    val fade = if (enabled) 1f else 0.45f
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) icon()
        BasicText(
            text,
            style = Type.Label.copy(
                color = if (kind == ButtonKind.Solid) Palette.Ink else Palette.TextHigh
            ),
        )
    }
    val inner = Modifier.padding(horizontal = 20.dp, vertical = 13.dp)
    val outer = modifier
        .graphicsLayer { alpha = fade }
        .pressable(enabled = enabled, onClick = onClick)

    when (kind) {
        ButtonKind.Solid -> Row(
            outer
                .background(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFE9E4DC))),
                    shape,
                )
                .then(inner),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

        ButtonKind.Quiet -> Row(
            outer.then(inner),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

        ButtonKind.Glass -> Glass(outer, shape = shape, backdrop = backdrop, tint = Color.White.copy(alpha = 0.10f)) {
            Row(
                inner.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Small glass status capsule with an optional glowing dot. */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    dotAlpha: Float = 1f,
    backdrop: Backdrop? = LocalBackdrop.current,
    style: TextStyle = Type.Label,
) {
    Glass(modifier, shape = CircleShape, backdrop = backdrop, tint = Color.White.copy(alpha = 0.08f)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (dot != null) {
                Box(
                    Modifier
                        .size(7.dp)
                        .graphicsLayer { alpha = dotAlpha }
                        .background(dot, CircleShape)
                )
            }
            BasicText(text, style = style)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
    content: @Composable () -> Unit,
) {
    Glass(modifier, shape = RoundedCornerShape(28.dp), backdrop = backdrop) {
        Box(Modifier.padding(20.dp)) { content() }
    }
}

// ---- Hand-drawn line icons (no icon font, no Material) -----------------------------------------

enum class Glyph { Camera, Prints, Guide, Trash, Close, Key, Retry, Photo, Check }

@Composable
fun Icon(glyph: Glyph, modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Palette.TextHigh) {
    Canvas(modifier.size(size)) { drawGlyph(glyph, color) }
}

private fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, p(x1, y1), p(x2, y2), strokeWidth = stroke.width, cap = StrokeCap.Round)

    when (glyph) {
        Glyph.Camera -> {
            drawRoundRect(color, p(0.08f, 0.26f), Size(s * 0.84f, s * 0.60f), CornerRadius(s * 0.14f), style = stroke)
            drawCircle(color, s * 0.15f, p(0.5f, 0.56f), style = stroke)
            line(0.36f, 0.26f, 0.42f, 0.14f); line(0.42f, 0.14f, 0.58f, 0.14f); line(0.58f, 0.14f, 0.64f, 0.26f)
            drawCircle(color, s * 0.035f, p(0.78f, 0.40f))
        }
        Glyph.Prints -> {
            drawRoundRect(color, p(0.26f, 0.08f), Size(s * 0.62f, s * 0.70f), CornerRadius(s * 0.07f), style = stroke)
            drawRoundRect(color, p(0.12f, 0.22f), Size(s * 0.62f, s * 0.70f), CornerRadius(s * 0.07f), style = stroke)
            drawRect(color.copy(alpha = 0.35f), p(0.20f, 0.30f), Size(s * 0.46f, s * 0.40f))
        }
        Glyph.Guide -> {
            drawCircle(color, s * 0.42f, p(0.5f, 0.5f), style = stroke)
            drawCircle(color, s * 0.05f, p(0.5f, 0.30f))
            line(0.5f, 0.45f, 0.5f, 0.72f)
        }
        Glyph.Trash -> {
            line(0.14f, 0.24f, 0.86f, 0.24f)
            line(0.38f, 0.24f, 0.40f, 0.10f); line(0.40f, 0.10f, 0.60f, 0.10f); line(0.60f, 0.10f, 0.62f, 0.24f)
            val path = Path().apply {
                moveTo(0.22f * s, 0.24f * s); lineTo(0.28f * s, 0.90f * s)
                lineTo(0.72f * s, 0.90f * s); lineTo(0.78f * s, 0.24f * s)
            }
            drawPath(path, color, style = stroke)
            line(0.42f, 0.40f, 0.43f, 0.74f); line(0.58f, 0.40f, 0.57f, 0.74f)
        }
        Glyph.Close -> {
            line(0.24f, 0.24f, 0.76f, 0.76f); line(0.76f, 0.24f, 0.24f, 0.76f)
        }
        Glyph.Key -> {
            drawCircle(color, s * 0.18f, p(0.30f, 0.62f), style = stroke)
            line(0.43f, 0.49f, 0.84f, 0.12f); line(0.70f, 0.25f, 0.80f, 0.36f); line(0.61f, 0.33f, 0.69f, 0.42f)
        }
        Glyph.Retry -> {
            drawArc(color, 40f, 290f, false, p(0.14f, 0.14f), Size(s * 0.72f, s * 0.72f), style = stroke)
            line(0.78f, 0.12f, 0.80f, 0.34f); line(0.80f, 0.34f, 0.58f, 0.34f)
        }
        Glyph.Photo -> {
            drawRoundRect(color, p(0.10f, 0.16f), Size(s * 0.80f, s * 0.68f), CornerRadius(s * 0.10f), style = stroke)
            drawCircle(color, s * 0.07f, p(0.34f, 0.38f))
            val path = Path().apply {
                moveTo(0.14f * s, 0.76f * s); lineTo(0.40f * s, 0.52f * s); lineTo(0.56f * s, 0.66f * s)
                lineTo(0.68f * s, 0.56f * s); lineTo(0.86f * s, 0.72f * s)
            }
            drawPath(path, color, style = stroke)
        }
        Glyph.Check -> {
            line(0.20f, 0.52f, 0.42f, 0.74f); line(0.42f, 0.74f, 0.82f, 0.28f)
        }
    }
}

/** Thumbs up (or, flipped, thumbs down). Outlined by default, solid when [filled]. */
@Composable
fun Thumb(up: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp, filled: Boolean = false, color: Color = Palette.TextHigh) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cuff = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0.08f * s, top = 0.44f * s, right = 0.27f * s, bottom = 0.92f * s,
                    cornerRadius = CornerRadius(0.04f * s),
                )
            )
        }
        val hand = Path().apply {
            moveTo(0.35f * s, 0.46f * s)
            lineTo(0.49f * s, 0.14f * s)
            quadraticBezierTo(0.53f * s, 0.06f * s, 0.60f * s, 0.10f * s)
            quadraticBezierTo(0.66f * s, 0.14f * s, 0.63f * s, 0.24f * s)
            lineTo(0.59f * s, 0.38f * s)
            lineTo(0.84f * s, 0.38f * s)
            quadraticBezierTo(0.94f * s, 0.40f * s, 0.91f * s, 0.50f * s)
            lineTo(0.83f * s, 0.84f * s)
            quadraticBezierTo(0.80f * s, 0.92f * s, 0.72f * s, 0.92f * s)
            lineTo(0.35f * s, 0.92f * s)
            close()
        }
        scale(scaleX = 1f, scaleY = if (up) 1f else -1f) {
            if (filled) {
                drawPath(cuff, color)
                drawPath(hand, color)
            } else {
                drawPath(cuff, color, style = stroke)
                drawPath(hand, color, style = stroke)
            }
        }
    }
}
