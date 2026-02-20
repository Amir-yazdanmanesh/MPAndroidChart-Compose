package com.github.mikephil.charting.compose.utils

import androidx.compose.ui.graphics.Color

/**
 * Predefined color palettes for chart styling.
 *
 * Provides the same color sets as the original MPAndroidChart ColorTemplate,
 * converted to Compose [Color] values.
 *
 * Usage:
 * ```
 * LineDataSet(
 *     entries = entries,
 *     config = DataSetConfig(colors = ChartColors.Material)
 * )
 * ```
 */
object ChartColors {

    /** Classic liberty-themed colors */
    val Liberty: List<Color> = listOf(
        Color(0xFF5DA5DA),
        Color(0xFFFAA43A),
        Color(0xFF60BD68),
        Color(0xFFF17CB0),
        Color(0xFFB2912F)
    )

    /** Joyful bright colors */
    val Joyful: List<Color> = listOf(
        Color(0xFFD4E157),
        Color(0xFF26C6DA),
        Color(0xFFFF7043),
        Color(0xFF9575CD),
        Color(0xFF66BB6A)
    )

    /** Soft pastel colors */
    val Pastel: List<Color> = listOf(
        Color(0xFF64B5F6),
        Color(0xFFE57373),
        Color(0xFFFFF176),
        Color(0xFF81C784),
        Color(0xFFBA68C8)
    )

    /** Vibrant colorful palette */
    val Colorful: List<Color> = listOf(
        Color(0xFFC12552),
        Color(0xFFFF6600),
        Color(0xFF11B651),
        Color(0xFF5528C5),
        Color(0xFF2196F3)
    )

    /** Classic Vordiplom earth tones */
    val Vordiplom: List<Color> = listOf(
        Color(0xFF7CBB94),
        Color(0xFF97BB7C),
        Color(0xFFBBB37C),
        Color(0xFFBB947C),
        Color(0xFFBB7C7C)
    )

    /** Material Design colors */
    val Material: List<Color> = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
        Color(0xFFFFEB3B),
        Color(0xFF795548)
    )

    /** Android ICS Holo Blue */
    val HoloBlue: Color = Color(0xFF0099CC)

    /**
     * Applies alpha to a color.
     *
     * @param color The base color
     * @param alpha Alpha value from 0 (transparent) to 255 (opaque)
     * @return Color with the specified alpha
     */
    fun withAlpha(color: Color, alpha: Int): Color {
        return color.copy(alpha = alpha.coerceIn(0, 255) / 255f)
    }

    /**
     * Creates a color from a hex string.
     *
     * @param hex Color string like "#FF0000" or "FF0000"
     * @return Parsed Color
     */
    fun fromHex(hex: String): Color {
        val cleaned = hex.removePrefix("#")
        val colorLong = cleaned.toLong(16)
        return when (cleaned.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> Color.Black
        }
    }

    /**
     * Generates a list of colors by cycling through a palette.
     *
     * @param palette The color palette to cycle through
     * @param count Number of colors needed
     * @return List of colors, cycling through the palette if count > palette.size
     */
    fun generate(palette: List<Color>, count: Int): List<Color> {
        return List(count) { palette[it % palette.size] }
    }
}
