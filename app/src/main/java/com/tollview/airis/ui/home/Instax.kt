package com.tollview.airis.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type
import kotlin.math.pow

/** Instax Square proportions, in millimetres. */
private const val FILM_W = 72f
private const val FILM_SIDE = 5f
private const val FILM_TOP = 7f
private const val FILM_IMAGE = 62f
private const val FILM_BOTTOM = 86f - FILM_TOP - FILM_IMAGE

/** How long a print takes to come up out of the black. Deliberately unhurried. */
const val EXPOSE_MILLIS = 9_500

/**
 * An instant-film print: warm paper, a recessed square of film, a blank bottom
 * border. [sheen] (−1…1) slides the glossy highlight, driven by tilt.
 */
@Composable
fun InstaxFrame(
    modifier: Modifier = Modifier,
    sheen: Float = 0f,
    elevated: Boolean = true,
    window: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier.aspectRatio(FILM_W / 86f)) {
        val mm = maxWidth / FILM_W
        val paperShape = RoundedCornerShape(mm * 1.2f)
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (elevated) Modifier.shadow(
                        elevation = mm * 7f,
                        shape = paperShape,
                        ambientColor = Color.Black,
                        spotColor = Color.Black,
                    ) else Modifier
                )
                .background(
                    Brush.linearGradient(
                        0f to Color(0xFFF8F5EE),
                        0.55f to Palette.Paper,
                        1f to Palette.PaperShade,
                    ),
                    paperShape,
                )
                .clip(paperShape)
        ) {
            // The film window, sunk slightly into the paper.
            Box(
                Modifier
                    .padding(start = mm * FILM_SIDE, top = mm * FILM_TOP)
                    .size(mm * FILM_IMAGE)
                    .clip(RoundedCornerShape(mm * 0.4f))
                    .background(Palette.FilmBlank)
            ) {
                window()
                // Inner edge shadow where the paper overlaps the film.
                Box(
                    Modifier
                        .matchParentSize()
                        .border(mm * 0.35f, Color.Black.copy(alpha = 0.22f))
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.22f),
                                    0.06f to Color.Transparent,
                                )
                            )
                        }
                )
            }

            // Gloss that glides across the print as it tilts.
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val x = size.width * (0.5f + sheen * 0.6f)
                        drawRect(
                            Brush.linearGradient(
                                0f to Color.Transparent,
                                0.45f to Color.White.copy(alpha = 0.00f),
                                0.52f to Color.White.copy(alpha = 0.16f),
                                0.60f to Color.Transparent,
                                start = Offset(x - size.width, 0f),
                                end = Offset(x + size.width * 0.4f, size.height),
                            )
                        )
                    }
            )
        }
    }
}

/** Fresh, unexposed film: a deep, faintly iridescent charcoal. */
@Composable
fun BoxScope.BlankFilm() {
    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to Color(0xFF262C33),
                        1f to Palette.FilmBlankEdge,
                        center = Offset(size.width * 0.4f, size.height * 0.35f),
                        radius = size.maxDimension,
                    )
                )
                drawRect(
                    Brush.linearGradient(
                        0f to Color(0x142FB6A8),
                        1f to Color(0x106C5CE0),
                    )
                )
            }
    )
}

/**
 * Skeleton loader, film-style: chemistry mottling slowly under the surface and a soft
 * band of light sweeping across, like watching a print that hasn't come up yet.
 */
@Composable
fun BoxScope.DevelopingFilm() {
    val transition = rememberInfiniteTransition(label = "develop")
    val sweep by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )

    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFF1E242A))

                fun mottle(color: Color, cx: Float, cy: Float, r: Float) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to color,
                            1f to Color.Transparent,
                            center = Offset(cx, cy),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
                val a = 0.10f + 0.10f * breathe
                mottle(Color(0xFF3E7C86).copy(alpha = a), w * (0.3f + 0.1f * kotlin.math.cos(drift)), h * 0.35f, w * 0.55f)
                mottle(Color(0xFF8A6A4A).copy(alpha = a * 0.8f), w * 0.75f, h * (0.65f + 0.1f * kotlin.math.sin(drift)), w * 0.5f)
                mottle(Color(0xFF5B4E86).copy(alpha = a * 0.7f), w * (0.55f + 0.15f * kotlin.math.sin(drift * 2)), h * 0.85f, w * 0.45f)

                // Skeleton shimmer band.
                val x = w * sweep
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.075f),
                        1f to Color.Transparent,
                        start = Offset(x - w * 0.35f, 0f),
                        end = Offset(x + w * 0.35f, h * 0.5f),
                    )
                )
            }
    )
}

/**
 * The print coming up out of the black: brightness climbs from zero, colour starts cold
 * and desaturated and warms into place, and the image sharpens as it settles, over
 * [EXPOSE_MILLIS]. With [animate] false it just shows the finished print.
 */
@Composable
fun BoxScope.ExposingFilm(
    image: ImageBitmap?,
    key: String,
    animate: Boolean,
    onExposed: () -> Unit,
) {
    val progress = remember(key) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(key, image != null) {
        if (image != null && progress.value < 1f) {
            progress.animateTo(
                1f,
                tween(EXPOSE_MILLIS, easing = CubicBezierEasing(0.45f, 0f, 0.2f, 1f)),
            )
            onExposed()
        }
    }
    if (image == null) {
        Box(Modifier.matchParentSize().background(Color.Black))
        return
    }
    val p = progress.value
    Image(
        bitmap = image,
        contentDescription = "Developed print",
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.colorMatrix(exposureMatrix(p)),
        modifier = Modifier
            .matchParentSize()
            .blur(((1f - p) * 8f).dp),
    )
}

/** Colour matrix for an instant print at exposure [p] (0 = black, 1 = fully developed). */
private fun exposureMatrix(p: Float): ColorMatrix {
    val brightness = p.pow(1.35f)
    val sat = 0.1f + 0.9f * smoothstep(0.3f, 1f, p)
    val contrast = 0.75f + 0.25f * p
    val cool = (1f - p) * 0.35f // early prints lean cyan
    val gR = brightness * (1f - cool * 0.8f) * contrast
    val gG = brightness * (1f + cool * 0.1f) * contrast
    val gB = brightness * (1f + cool * 0.5f) * contrast
    val offset = 255f * 0.5f * (1f - contrast) * brightness

    val lr = 0.213f * (1 - sat)
    val lg = 0.715f * (1 - sat)
    val lb = 0.072f * (1 - sat)
    return ColorMatrix(
        floatArrayOf(
            (lr + sat) * gR, lg * gR, lb * gR, 0f, offset,
            lr * gG, (lg + sat) * gG, lb * gG, 0f, offset,
            lr * gB, lg * gB, (lb + sat) * gB, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}
