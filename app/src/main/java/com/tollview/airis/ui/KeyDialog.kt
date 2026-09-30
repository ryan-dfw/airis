package com.tollview.airis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tollview.airis.ui.components.ButtonKind
import com.tollview.airis.ui.components.GlassButton
import com.tollview.airis.ui.components.pressable
import com.tollview.airis.ui.glass.Backdrop
import com.tollview.airis.ui.glass.Glass
import com.tollview.airis.ui.theme.Palette
import com.tollview.airis.ui.theme.Type

/** Glass sheet for pasting in an OpenAI API key. */
@Composable
fun KeyDialog(
    scene: Backdrop,
    firstRun: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val trimmed = value.trim()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pressable(pressedScale = 1f, onClick = onDismiss)
            .imePadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Glass(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .pressable(pressedScale = 1f) {}, // keep taps inside the sheet
            shape = RoundedCornerShape(32.dp),
            backdrop = scene,
            blur = 36.dp,
            tint = Color.White.copy(alpha = 0.10f),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(if (firstRun) "Add API key" else "Change API key", style = Type.Title.copy(fontSize = Type.Heading.fontSize * 1.35f))
                BasicText(
                    if (firstRun) {
                        "Paste your OpenAI API key to start developing. It's encrypted and stored on this device."
                    } else {
                        "Paste a new OpenAI API key. It replaces the saved one."
                    },
                    style = Type.Body,
                )
                Spacer(Modifier.height(4.dp))

                val fieldShape = RoundedCornerShape(16.dp)
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = Type.Mono.copy(color = Palette.TextHigh),
                    cursorBrush = SolidColor(Palette.Ember),
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (trimmed.isNotEmpty()) onSave(trimmed) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    decorationBox = { inner ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.28f), fieldShape)
                                .border(1.dp, Color.White.copy(alpha = 0.14f), fieldShape)
                                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                if (value.isEmpty()) {
                                    BasicText("Paste key", style = Type.Mono.copy(color = Palette.TextLow))
                                }
                                inner()
                            }
                            BasicText(
                                if (value.isEmpty()) "Paste" else if (reveal) "Hide" else "Show",
                                style = Type.Label.copy(color = Palette.TextMid),
                                modifier = Modifier
                                    .pressable {
                                        if (value.isEmpty()) {
                                            clipboard.getText()?.text?.let { value = it.trim() }
                                        } else {
                                            reveal = !reveal
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    },
                )


                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    GlassButton(if (firstRun) "Later" else "Cancel", kind = ButtonKind.Quiet, backdrop = scene, onClick = onDismiss)
                    GlassButton(
                        "Save",
                        kind = ButtonKind.Solid,
                        enabled = trimmed.isNotEmpty(),
                        backdrop = scene,
                        onClick = { onSave(trimmed) },
                    )
                }
            }
        }
    }
}
