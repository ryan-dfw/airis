package com.tollview.airis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tollview.airis.AirisViewModel
import com.tollview.airis.data.Print
import com.tollview.airis.ui.gallery.GalleryScreen
import com.tollview.airis.ui.gallery.PrintViewer
import com.tollview.airis.ui.glass.AmbientBackdrop
import com.tollview.airis.ui.glass.Backdrop
import com.tollview.airis.ui.glass.Glass
import com.tollview.airis.ui.glass.LocalBackdrop
import com.tollview.airis.ui.glass.backdropSource
import com.tollview.airis.ui.glass.rememberBackdrop
import com.tollview.airis.ui.guide.GuideScreen
import com.tollview.airis.ui.home.HomeScreen

@Composable
fun AirisApp(vm: AirisViewModel) {
    val apiKey by vm.apiKey.collectAsStateWithLifecycle()
    val film by vm.film.collectAsStateWithLifecycle()
    val camera by vm.camera.collectAsStateWithLifecycle()
    val prints by vm.prints.collectAsStateWithLifecycle()
    val exposed by vm.exposed.collectAsStateWithLifecycle()
    val ejected by vm.ejected.collectAsStateWithLifecycle()
    val ratings by vm.ratings.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Camera) }
    // First launch with no key: open the key sheet straight away.
    var showKeyDialog by rememberSaveable { mutableStateOf(apiKey.isNullOrBlank()) }
    var viewingId by rememberSaveable { mutableStateOf<String?>(null) }
    val viewing = prints.firstOrNull { it.id == viewingId }
    var lastViewing by remember { mutableStateOf<Print?>(null) }
    LaunchedEffect(viewing) { if (viewing != null) lastViewing = viewing }

    BackHandler(enabled = showKeyDialog || viewing != null || tab != Tab.Camera) {
        when {
            showKeyDialog -> showKeyDialog = false
            viewing != null -> viewingId = null
            else -> tab = Tab.Camera
        }
    }

    val ambient = rememberBackdrop()
    val scene = rememberBackdrop()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = (navBottom.value + TabBarHeight.value + 20f).toInt()

    Box(Modifier.fillMaxSize()) {
        // Everything in here is "the scene": overlays above it can frost it.
        Box(Modifier.fillMaxSize().backdropSource(scene)) {
            AmbientBackdrop(ambient)
            CompositionLocalProvider(LocalBackdrop provides ambient) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.985f)) togetherWith
                            fadeOut(tween(200))
                    },
                    label = "tabs",
                ) { current ->
                    when (current) {
                        Tab.Camera -> HomeScreen(
                            film = film,
                            camera = camera,
                            hasKey = !apiKey.isNullOrBlank(),
                            exposed = exposed,
                            onExposed = vm::markExposed,
                            ejected = ejected,
                            onEjected = vm::markEjected,
                            ratings = ratings,
                            onRate = vm::rate,
                            onAddKey = { showKeyDialog = true },
                            onRetry = vm::retry,
                            onDismiss = vm::eject,
                            bottomInset = bottomInset,
                        )

                        Tab.Prints -> GalleryScreen(
                            prints = prints,
                            ratings = ratings,
                            onOpen = { viewingId = it.id },
                            bottomInset = bottomInset,
                        )

                        Tab.Setup -> GuideScreen(
                            apiKey = apiKey,
                            onEditKey = { showKeyDialog = true },
                            onRemoveKey = vm::clearApiKey,
                            bottomInset = bottomInset,
                        )
                    }
                }
            }
        }

        TabBar(
            selected = tab,
            onSelect = { tab = it },
            scene = scene,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )

        Overlay(visible = viewing != null, scene = scene) {
            // Keep showing the last print while the overlay fades out.
            (viewing ?: lastViewing)?.let { print ->
                PrintViewer(
                    print = print,
                    scene = scene,
                    rating = ratings[print.id],
                    onRate = vm::rate,
                    onClose = { viewingId = null },
                    onDelete = {
                        vm.deletePrint(it)
                        viewingId = null
                    },
                )
            }
        }

        Overlay(visible = showKeyDialog, scene = scene) {
            KeyDialog(
                scene = scene,
                firstRun = apiKey.isNullOrBlank(),
                onSave = {
                    vm.saveApiKey(it)
                    showKeyDialog = false
                },
                onDismiss = { showKeyDialog = false },
            )
        }
    }
}

/** Full-screen overlay that frosts the whole scene behind it. */
@Composable
private fun Overlay(visible: Boolean, scene: Backdrop, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)),
        exit = fadeOut(tween(220)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Glass(
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                backdrop = scene,
                blur = 34.dp,
                tint = Color.Black.copy(alpha = 0.25f),
                rim = 0f,
            ) {}
            Box(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = scaleIn(tween(360), initialScale = 0.94f),
                        exit = scaleOut(tween(220), targetScale = 0.97f),
                    )
            ) {
                content()
            }
        }
    }
}
