package com.tollview.airis.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tollview.airis.ui.components.ButtonKind
import com.tollview.airis.ui.components.GlassButton
import com.tollview.airis.ui.components.GlassCard
import com.tollview.airis.ui.components.Glyph
import com.tollview.airis.ui.components.Icon
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type

@Composable
fun GuideScreen(
    apiKey: String?,
    onEditKey: () -> Unit,
    onRemoveKey: () -> Unit,
    bottomInset: Int,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = bottomInset.dp + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText("Setup", style = Type.Title, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))

        // --- How it works ----------------------------------------------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Glyph.Photo, "How AIris works")
                Body("AIris takes photos with AI. Your camera is only its lens. Point and shoot - with no need to frame or focus, you can trust AI to capture the scene perfectly. Every creation is saved in Prints; nothing else is needed.")
            }
        }

        // --- Camera ----------------------------------------------------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle(Glyph.Camera, "Connect your camera")
                Body("AIris works with the Sony QX1, QX10, QX30 and QX100 over Wi-Fi Direct.")
                Step(1, "Insert a charged battery and a microSD card into the camera.")
                Step(2, "Press the power button. The camera starts its own Wi-Fi network.")
                Step(3, "On your phone, open Settings › Wi-Fi and join the network named after your camera, such as DIRECT-xxxx:DSC-QX1. The password is on the label inside the battery cover.")
                Step(4, "If Android says the network has no internet access, choose to stay connected.")
                Step(5, "Open AIris. When the eject slot lights up, the camera is ready. Press the shutter.")
            }
        }

        // --- Mobile data -----------------------------------------------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Glyph.Guide, "Mobile data")
                Body("Keep mobile data on. While your phone is joined to the camera over Wi-Fi Direct, AIris keeps a separate cellular connection open and sends each shot over it for processing. You don't need to switch networks between shots.")
            }
        }

        // --- API key ---------------------------------------------------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(Glyph.Key, "API key")
                if (apiKey.isNullOrBlank()) {
                    Body("Not set")
                    GlassButton("Add key", kind = ButtonKind.Solid, onClick = onEditKey)
                } else {
                    Body("Saved on this device")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Change", onClick = onEditKey)
                        GlassButton("Remove", kind = ButtonKind.Quiet, onClick = onRemoveKey)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(glyph: Glyph, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(32.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(glyph, size = 17.dp)
        }
        BasicText(title, style = Type.Heading)
    }
}

@Composable
private fun Body(text: String) {
    BasicText(text, style = Type.Body)
}

@Composable
private fun Step(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("$n", style = Type.Caption.copy(color = Palette.TextHigh, fontWeight = FontWeight.SemiBold))
        }
        BasicText(text, modifier = Modifier.weight(1f).padding(top = 2.dp), style = Type.Body)
    }
}
