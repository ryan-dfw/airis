package com.tollview.airis.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tollview.airis.data.Print
import com.tollview.airis.data.Rating
import com.tollview.airis.ui.components.GlassButton
import com.tollview.airis.ui.components.GlassCard
import com.tollview.airis.ui.components.Glyph
import com.tollview.airis.ui.components.Icon
import com.tollview.airis.ui.components.Thumb
import com.tollview.airis.ui.components.pressable
import com.tollview.airis.ui.components.rememberImage
import com.tollview.airis.ui.glass.Backdrop
import com.tollview.airis.ui.glass.Glass
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type
import kotlinx.coroutines.delay

/** Plain square grid of everything developed, newest first. */
@Composable
fun GalleryScreen(
    prints: List<Print>,
    ratings: Map<String, Rating>,
    onOpen: (Print) -> Unit,
    bottomInset: Int,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = top + 14.dp,
            bottom = bottomInset.dp + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(start = 12.dp, bottom = 14.dp)) {
                BasicText("Prints", style = Type.Title)
                Spacer(Modifier.height(4.dp))
                BasicText(
                    when (prints.size) {
                        0 -> "No prints yet"
                        1 -> "1 print"
                        else -> "${prints.size} prints"
                    },
                    style = Type.Caption,
                )
            }
        }

        if (prints.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    BasicText(
                        "Developed prints are kept here.",
                        modifier = Modifier.fillMaxWidth(),
                        style = Type.Body.copy(textAlign = TextAlign.Center),
                    )
                }
            }
        }

        items(prints, key = { it.id }) { print ->
            Box(
                Modifier
                    .animateItem()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Palette.InkSoft)
                    .pressable(pressedScale = 0.96f) { onOpen(print) }
            ) {
                val image by rememberImage(print.file, 420)
                image?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Print",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                ratings[print.id]?.let { rating ->
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(22.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Thumb(up = rating == Rating.Like, filled = true, size = 12.dp)
                    }
                }
            }
        }
    }
}

/** One print, large, over a darkened and frosted scene. */
@Composable
fun PrintViewer(
    print: Print,
    scene: Backdrop,
    rating: Rating?,
    onRate: (Print, Rating) -> Unit,
    onClose: () -> Unit,
    onDelete: (Print) -> Unit,
) {
    var confirmDelete by remember(print.id) { mutableStateOf(false) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(3_000)
            confirmDelete = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pressable(pressedScale = 1f, onClick = onClose)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GlassButton(
                    "Close",
                    backdrop = scene,
                    icon = { Icon(Glyph.Close, size = 14.dp) },
                    onClick = onClose,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Palette.InkSoft)
                    // Swallow taps on the image so they don't close the viewer.
                    .pressable(pressedScale = 1f) {}
            ) {
                val image by rememberImage(print.file, 1600)
                image?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Print",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewerRate(up = false, selected = rating == Rating.Dislike, scene = scene) { onRate(print, Rating.Dislike) }
                ViewerRate(up = true, selected = rating == Rating.Like, scene = scene) { onRate(print, Rating.Like) }
                GlassButton(
                    if (confirmDelete) "Tap again to delete" else "Delete",
                    backdrop = scene,
                    icon = {
                        Icon(
                            Glyph.Trash,
                            size = 16.dp,
                            color = if (confirmDelete) Palette.Danger else Palette.TextHigh,
                        )
                    },
                    onClick = {
                        if (confirmDelete) onDelete(print) else confirmDelete = true
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ViewerRate(up: Boolean, selected: Boolean, scene: Backdrop, onClick: () -> Unit) {
    Glass(
        modifier = Modifier
            .size(48.dp)
            .pressable(pressedScale = 0.88f, onClick = onClick),
        shape = CircleShape,
        backdrop = scene,
        tint = Color.White.copy(alpha = if (selected) 0.22f else 0.08f),
    ) {
        Thumb(
            up = up,
            filled = selected,
            size = 20.dp,
            color = if (selected) Palette.TextHigh else Palette.TextMid,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
