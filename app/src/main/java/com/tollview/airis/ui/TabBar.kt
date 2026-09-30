package com.tollview.airis.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tollview.airis.ui.components.Glyph
import com.tollview.airis.ui.components.Icon
import com.tollview.airis.ui.components.pressable
import com.tollview.airis.ui.glass.Backdrop
import com.tollview.airis.ui.glass.Glass
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type

enum class Tab(val label: String, val glyph: Glyph) {
    Prints("Prints", Glyph.Prints),
    Camera("Camera", Glyph.Camera),
    Setup("Setup", Glyph.Guide),
}

val TabBarHeight = 64.dp

/** Floating liquid-glass capsule; the selected lens slides between tabs on a springy spring. */
@Composable
fun TabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    scene: Backdrop,
    modifier: Modifier = Modifier,
) {
    Glass(
        modifier = modifier
            .width(292.dp)
            .height(TabBarHeight),
        shape = CircleShape,
        backdrop = scene,
        blur = 22.dp,
        tint = Color.White.copy(alpha = 0.06f),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(5.dp)) {
            val itemWidth = maxWidth / Tab.entries.size
            val lensX by animateDpAsState(
                targetValue = itemWidth * selected.ordinal,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
                label = "lens",
            )
            // The lens: a brighter bead of glass under the active tab.
            Box(
                Modifier
                    .offset(x = lensX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
                        ),
                        CircleShape,
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            )
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Tab.entries.forEach { tab ->
                    val active = tab == selected
                    val tint by animateColorAsState(
                        if (active) Palette.TextHigh else Palette.TextLow,
                        label = "tint",
                    )
                    Column(
                        Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .pressable(pressedScale = 0.9f) { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.glyph, size = 20.dp, color = tint)
                        BasicText(
                            tab.label,
                            style = Type.Caption.copy(color = tint, fontSize = 11.sp),
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
