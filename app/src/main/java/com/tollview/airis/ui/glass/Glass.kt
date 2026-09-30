package com.tollview.airis.ui.glass

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tollview.airis.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A recorded snapshot of whatever is drawn by a [backdropSource]. Glass panels draw a
 * blurred, position-matched copy of it behind themselves, which is what makes them
 * read as frosted glass sitting on top of the scene.
 */
class Backdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/** The backdrop glass inside screen content samples (the ambient light only). */
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Records this node's drawing into [backdrop] and draws it as normal. */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInWindow() }
    .drawWithContent {
        backdrop.layer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(backdrop.layer)
    }

private val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Liquid glass surface: blurred backdrop, a faint milky tint, a bright specular
 * rim that fades around the edge, and a soft top sheen.
 *
 * Must not sample a backdrop that it is itself part of (the scene backdrop is only
 * for overlays drawn outside the scene, like the tab bar and dialogs).
 */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape,
    backdrop: Backdrop? = LocalBackdrop.current,
    blur: Dp = 28.dp,
    tint: Color = Color.White.copy(alpha = 0.07f),
    rim: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    var position by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .onGloballyPositioned { position = it.positionInWindow() }
            .clip(shape)
    ) {
        if (backdrop != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        clip = true
                        if (canBlur) {
                            val px = blur.toPx()
                            renderEffect = BlurEffect(px, px, TileMode.Clamp)
                        }
                    }
                    .drawBehind {
                        val offset = position - backdrop.origin
                        translate(-offset.x, -offset.y) {
                            drawLayer(backdrop.layer)
                        }
                    }
            )
        }
        // Without real blur (Android 11 and below) lean on a denser frosted tint instead.
        Box(
            Modifier
                .matchParentSize()
                .background(if (canBlur) Color.Transparent else Palette.InkSoft.copy(alpha = 0.78f))
                .background(tint)
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.10f),
                        0.45f to Color.White.copy(alpha = 0.02f),
                        1f to Color.White.copy(alpha = 0.04f),
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.55f * rim),
                        0.35f to Color.White.copy(alpha = 0.10f * rim),
                        0.7f to Color.White.copy(alpha = 0.05f * rim),
                        1f to Color.White.copy(alpha = 0.28f * rim),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                    shape = shape,
                )
        )
        content()
    }
}

/**
 * The darkroom behind everything: slow drifting pools of warm and cool light plus a
 * little film grain.
 */
@Composable
fun AmbientLight(modifier: Modifier = Modifier, warmth: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(48_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )
    val grain = remember { grainBrush() }

    Canvas(modifier.fillMaxSize()) {
        drawRect(Palette.Ink)
        val w = size.width
        val h = size.height
        val r = maxOf(w, h)

        fun blob(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
            val center = Offset(cx, cy)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = alpha),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        blob(Palette.Violet, w * (0.15f + 0.10f * cos(t)), h * (0.18f + 0.06f * sin(t * 2)), r * 0.62f, 0.55f)
        blob(Palette.Ember, w * (0.92f + 0.08f * sin(t)), h * (0.42f + 0.10f * cos(t)), r * 0.55f, 0.42f * warmth)
        blob(Palette.Rose, w * (0.30f + 0.12f * sin(t + 1.3f)), h * (0.86f + 0.05f * cos(t * 2)), r * 0.58f, 0.40f)
        blob(Palette.Teal, w * (0.85f + 0.10f * cos(t + 2.1f)), h * (0.98f + 0.04f * sin(t)), r * 0.45f, 0.30f)

        // Vignette.
        drawRect(
            Brush.radialGradient(
                0.55f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.55f),
                center = Offset(w / 2, h * 0.45f),
                radius = r * 0.85f,
            )
        )
        drawRect(grain, alpha = 0.06f)
    }
}

private fun grainBrush(): ShaderBrush {
    val size = 160
    val random = Random(7)
    val pixels = IntArray(size * size) {
        val v = random.nextInt(256)
        (random.nextInt(90) shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    val image: ImageBitmap = bitmap.asImageBitmap()
    return ShaderBrush(ImageShader(image, TileMode.Repeated, TileMode.Repeated))
}

/** Convenience: a full-bleed ambient backdrop that also records itself for glass. */
@Composable
fun AmbientBackdrop(backdrop: Backdrop, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().backdropSource(backdrop)) {
        AmbientLight()
    }
}
