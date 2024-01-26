package dev.gerlot.screenlit.util

import android.graphics.Color
import androidx.annotation.ColorInt

enum class ColorDarkness {
    BRIGHT, DARK
}

fun calculateColorDarkness(@ColorInt color: Int): ColorDarkness {
    val luminance = calculateColorLuminance(color)
    return if (luminance < 0.5) {
        ColorDarkness.BRIGHT
    } else {
        ColorDarkness.DARK
    }
}

fun calculateColorLuminance(@ColorInt color: Int): Double {
    return 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
}
