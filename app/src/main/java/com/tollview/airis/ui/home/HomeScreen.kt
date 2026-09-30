package com.tollview.airis.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tollview.airis.Film
import com.tollview.airis.data.Print
import com.tollview.airis.pipeline.Pipeline
import com.tollview.airis.ui.components.ButtonKind
import com.tollview.airis.ui.components.GlassButton
import com.tollview.airis.ui.components.GlassPill
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type
import com.tollview.airis.ui.components.rememberImage
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.tollview.airis.sheetId
import com.tollview.airis.ui.glass.Glass
import kotlinx.coroutines.delay
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.tollview.airis.data.Rating
import com.tollview.airis.ui.components.Thumb
import com.tollview.airis.ui.components.AirisMark
import com.tollview.airis.ui.components.pressable
import kotlinx.coroutines.launch

/**
 * The camera tab. Deliberately quiet: no print until the camera hands us a photo, one
 * word while it develops, and text only when something is actually wrong.
 */
@Composable
fun HomeScreen(
    film: Film,
    camera: Pipeline.CameraStatus,
    hasKey: Boolean,
    exposed: Set<String>,
    onExposed: (Print) -> Unit,
    ejected: Long,
    onEjected: (Long) -> Unit,
    ratings: Map<String, Rating>,
    onRate: (Print, Rating) -> Unit,
    onAddKey: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    bottomInset: Int,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = bottomInset.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(problem = problemFor(film, camera, hasKey))

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Narrow enough to leave a gutter either side for the rating buttons.
            val printWidth = minOf(maxWidth * 0.64f, 360.dp, (maxHeight - 90.dp) / 1.22f)
            HollowStage(
                film = film,
                listening = hasKey && camera == Pipeline.CameraStatus.Connected,
                exposed = exposed,
                onExposed = onExposed,
                ejected = ejected,
                onEjected = onEjected,
                ratings = ratings,
                onRate = onRate,
                printWidth = printWidth,
                areaWidth = maxWidth,
                areaHeight = maxHeight,
            )

            if (film is Film.Blank && !hasKey) {
                GlassButton(
                    "Add API key",
                    kind = ButtonKind.Solid,
                    onClick = onAddKey,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 72.dp),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = film,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "caption",
            ) { shown ->
                when (shown) {
                    is Film.Developing -> Developing()
                    is Film.Failed -> Failure(shown, onRetry, onDismiss)
                    else -> Box(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** A header pill only when something needs attention; otherwise just the wordmark. */
private fun problemFor(film: Film, camera: Pipeline.CameraStatus, hasKey: Boolean): String? = when {
    !hasKey -> "No API key"
    film is Film.Blank && camera == Pipeline.CameraStatus.Searching -> "No camera"
    else -> null
}

@Composable
private fun Header(problem: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText("AIris", style = Type.Wordmark)
        AnimatedVisibility(visible = problem != null, enter = fadeIn(), exit = fadeOut()) {
            GlassPill(text = problem.orEmpty(), dot = Palette.Warn)
        }
    }
}

@Composable
private fun Developing() {
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Reverse),
        label = "alpha",
    )
    BasicText(
        "developing...",
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = breathe },
        style = Type.Label.copy(color = Palette.TextMid, textAlign = TextAlign.Center),
    )
}

@Composable
private fun Failure(film: Film.Failed, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            film.message,
            style = Type.Body.copy(color = Palette.TextHigh, textAlign = TextAlign.Center),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (film.canRetry) {
                GlassButton("Try again", kind = ButtonKind.Solid, onClick = onRetry)
            }
            GlassButton("Dismiss", onClick = onDismiss)
        }
    }
}

/** How long a print takes to come out of the hollow. */
private const val EMERGE_MILLIS = 1_900

private data class Sheet(val id: Long?, val film: Film)

/**
 * The home stage: a dark, breathing hollow with the AIris mark floating in it. When the
 * camera hands over a photo, the print comes spinning up out of the hollow and settles
 * in front of it to develop. When it goes, it's drawn back in.
 */
@Composable
private fun HollowStage(
    film: Film,
    listening: Boolean,
    exposed: Set<String>,
    onExposed: (Print) -> Unit,
    ejected: Long,
    onEjected: (Long) -> Unit,
    ratings: Map<String, Rating>,
    onRate: (Print, Rating) -> Unit,
    printWidth: Dp,
    areaWidth: Dp,
    areaHeight: Dp,
) {
    val printHeight = printWidth * (86f / 72f)
    val printTop = (areaHeight - printHeight) / 2
    val hollow = printWidth * 1.3f
    // The mark is dimmer while there's no camera to listen to.
    val markAlpha by animateFloatAsState(if (listening) 1f else 0.5f, tween(900), label = "markAlpha")

    Box(Modifier.fillMaxSize()) {
        // 1. The hollow: static, centred, beneath everything.
        VoidShadow(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = areaHeight / 2 - hollow / 2)
                .size(hollow)
        )

        // 2. The mark, floating in the hollow.
        val mark = hollow * 0.62f
        AirisMark(
            alpha = markAlpha,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = areaHeight / 2 - mark / 2)
                .size(mark)
        )

        // 3. The print, centred over the hollow.
        AnimatedContent(
            targetState = Sheet(film.sheetId, film),
            contentKey = { it.id },
            contentAlignment = Alignment.Center,
            transitionSpec = {
                // Arrivals animate themselves (EmergingPrint); departures are drawn back into the hollow.
                EnterTransition.None togetherWith
                    (fadeOut(tween(600)) + scaleOut(tween(600, easing = CubicBezierEasing(0.5f, 0f, 0.9f, 0.6f)), targetScale = 0.05f)) using
                    SizeTransform(clip = false)
            },
            modifier = Modifier.fillMaxSize(),
            label = "sheet",
        ) { sheet ->
            val id = sheet.id
            if (id == null) {
                Box(Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmergingPrint(
                        film = sheet.film,
                        sheet = id,
                        animate = id != ejected,
                        printWidth = printWidth,
                        exposed = exposed,
                        onExposed = onExposed,
                        onEjected = onEjected,
                    )
                }
            }
        }

        // 4. Thumbs either side of the print, once it has fully come up.
        val printed = film as? Film.Printed
        val gutter = (areaWidth - printWidth) / 2
        val filmCentre = printTop + printWidth * (38f / 72f)
        AnimatedVisibility(
            visible = printed != null && printed.print.id in exposed,
            enter = fadeIn(tween(700)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize(),
        ) {
            val print = printed?.print
            val rating = print?.let { ratings[it.id] }
            if (print != null) Box(Modifier.fillMaxSize()) {
                RateButton(
                    up = false,
                    selected = rating == Rating.Dislike,
                    onClick = { onRate(print, Rating.Dislike) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = -(printWidth / 2 + gutter / 2), y = filmCentre - 26.dp),
                )
                RateButton(
                    up = true,
                    selected = rating == Rating.Like,
                    onClick = { onRate(print, Rating.Like) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = printWidth / 2 + gutter / 2, y = filmCentre - 26.dp),
                )
            }
        }
    }
}

/** Where the print should be: a dim hollow that slowly breathes, with a faint ring of light circling it. */
@Composable
private fun VoidShadow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hollow")
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_600), RepeatMode.Reverse),
        label = "breathe",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(
            Brush.radialGradient(
                0f to Color.Black.copy(alpha = 0.50f + 0.18f * breathe),
                0.55f to Color.Black.copy(alpha = 0.22f + 0.08f * breathe),
                1f to Color.Transparent,
                center = center,
                radius = r,
            ),
            radius = r,
        )
        rotate(spin) {
            drawCircle(
                Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.18f to Color.White.copy(alpha = 0.06f + 0.08f * breathe),
                    0.36f to Color.Transparent,
                    0.66f to Palette.Ember.copy(alpha = 0.05f + 0.06f * breathe),
                    0.85f to Color.Transparent,
                    center = center,
                ),
                radius = r * (0.58f + 0.03f * breathe),
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun RateButton(up: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Glass(
        modifier = modifier
            .size(52.dp)
            .pressable(pressedScale = 0.88f) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = CircleShape,
        tint = Color.White.copy(alpha = if (selected) 0.22f else 0.07f),
    ) {
        Thumb(
            up = up,
            filled = selected,
            size = 22.dp,
            color = if (selected) Palette.TextHigh else Palette.TextMid,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** A print spinning up out of the hollow, then settling in place to develop. */
@Composable
private fun EmergingPrint(
    film: Film,
    sheet: Long,
    animate: Boolean,
    printWidth: Dp,
    exposed: Set<String>,
    onExposed: (Print) -> Unit,
    onEjected: (Long) -> Unit,
) {
    val progress = remember(sheet) { Animatable(if (animate) 0f else 1f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(sheet) {
        if (progress.value < 1f) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            progress.animateTo(1f, tween(EMERGE_MILLIS, easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)))
        }
        onEjected(sheet)
    }
    val p = progress.value

    TiltingPrint(
        film = film,
        settled = p >= 1f,
        exposed = exposed,
        onExposed = onExposed,
        modifier = Modifier
            .width(printWidth)
            .graphicsLayer {
                // Out of the dark: grows from a point, unwinds a half turn, fades up.
                val scale = 0.04f + 0.96f * p
                scaleX = scale
                scaleY = scale
                rotationZ = -180f * (1f - p)
                alpha = (p * 1.6f).coerceAtMost(1f)
            },
    )
}

@Composable
private fun TiltingPrint(
    film: Film,
    settled: Boolean,
    exposed: Set<String>,
    onExposed: (Print) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    // Idle float, eased in once the sheet is fully out.
    val bobAmount by animateFloatAsState(if (settled) 1f else 0f, tween(1_400), label = "bobAmount")
    val bobPhase by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_800), RepeatMode.Reverse),
        label = "bob",
    )
    val bob = bobPhase * bobAmount

    Box(
        modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { rotX.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 120f)) }
                        scope.launch { rotY.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 120f)) }
                    },
                    onDragCancel = {
                        scope.launch { rotX.animateTo(0f, spring()) }
                        scope.launch { rotY.animateTo(0f, spring()) }
                    },
                ) { change, drag ->
                    change.consume()
                    scope.launch { rotY.snapTo((rotY.value + drag.x * 0.06f).coerceIn(-16f, 16f)) }
                    scope.launch { rotX.snapTo((rotX.value - drag.y * 0.06f).coerceIn(-16f, 16f)) }
                }
            }
            .graphicsLayer {
                rotationX = rotX.value + bob * 0.8f
                rotationY = rotY.value
                rotationZ = (-1.6f + rotY.value * 0.05f) * bobAmount
                translationY = bob * 4.dp.toPx()
                cameraDistance = 14f * density
            }
    ) {
        InstaxFrame(sheen = (rotY.value / 16f) + bob * 0.08f) {
            when (film) {
                Film.Blank, is Film.Failed -> BlankFilm()
                is Film.Developing -> DevelopingFilm()
                is Film.Printed -> {
                    val image by rememberImage(film.print.file, 1280)
                    ExposingFilm(
                        image = image,
                        key = film.print.id,
                        animate = film.print.id !in exposed,
                        onExposed = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onExposed(film.print)
                        },
                    )
                }
            }
        }
    }
}
