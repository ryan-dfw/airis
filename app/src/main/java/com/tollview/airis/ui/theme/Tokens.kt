package com.tollview.airis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Colours: a warm, dim darkroom with light pouring through glass. */
object Palette {
    val Ink = Color(0xFF0E0D10)
    val InkSoft = Color(0xFF17151B)

    // Ambient light blobs behind the glass.
    val Ember = Color(0xFFF08A4B)
    val Rose = Color(0xFFD9577A)
    val Violet = Color(0xFF6C5CE0)
    val Teal = Color(0xFF2FB6A8)

    // Text on glass.
    val TextHigh = Color(0xFFF7F4EF)
    val TextMid = Color(0xB3F7F4EF)
    val TextLow = Color(0x73F7F4EF)

    // Instant film.
    val Paper = Color(0xFFF3EFE6)
    val PaperShade = Color(0xFFE4DED1)
    val FilmBlank = Color(0xFF1B1F24)
    val FilmBlankEdge = Color(0xFF12151A)

    val Live = Color(0xFF5BE38C)
    val Warn = Color(0xFFFFB35C)
    val Danger = Color(0xFFFF6B6B)
}

object Type {
    val Wordmark = TextStyle(
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        letterSpacing = (-0.02).em,
        color = Palette.TextHigh,
    )
    val Title = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.02).em,
        color = Palette.TextHigh,
    )
    val Heading = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.01).em,
        color = Palette.TextHigh,
    )
    val Body = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Palette.TextMid,
    )
    val Label = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.01.em,
        color = Palette.TextHigh,
    )
    val Caption = TextStyle(
        fontSize = 12.sp,
        letterSpacing = 0.02.em,
        color = Palette.TextLow,
    )
    val Mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = Palette.TextHigh,
    )
}
