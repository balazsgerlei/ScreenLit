package dev.gerlot.screenlit.util

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ScreenBrightnessManager {

    private var screenBrightnessChangeStart: Float? = null
    private var screenBrightnessAtChangeStart: Float? = null

    val changingBrightness: Boolean
        get() = screenBrightnessChangeStart != null

    fun onStartScreenBrightnessChange(y: Float, currentScreenBrightness: Float?) {
        currentScreenBrightness?.let { brightness ->
            screenBrightnessChangeStart = y

            // The brightness value needs to be linearized for the starting point
            // because it is squared when setting the new value for gamma correction
            val linearizedBrightness = sqrt(brightness)

            // We need to account for the brightness floor we used (not allowing setting the screen
            // brightness below a certain value) and convert it to 0 so the change starts from that
            screenBrightnessAtChangeStart = linearizedBrightness
                .takeUnless { it <= BRIGHTNESS_FLOOR }?.let {
                    (it * 1000f).roundToInt() / 1000f
                } ?: 0f
        }
    }

    fun onScreenBrightnessChangeRequest(
        viewHeight: Int,
        y: Float,
        onChangeScreenBrightness: (newBrightness: Float, newProgress: Float) -> Unit
    ) {
        screenBrightnessChangeStart?.let { start ->
            val normalizedStart = calculateNormalizedScreenPosition(start, viewHeight)
            if (!isSmallMove(start, y, viewHeight)) { // Ignore small movement that can be an imprecise tap
                val normalizedScreenPosition = calculateNormalizedScreenPosition(y, viewHeight)
                val screenBrightnessChange = ((normalizedScreenPosition - normalizedStart) * 1000f).roundToInt() / 1000f

                val previousBrightness = screenBrightnessAtChangeStart
                previousBrightness?.let {
                    val calculatedBrightness = (((it + screenBrightnessChange) * 1000f).roundToInt() / 1000f).coerceIn(0f, 1f)

                    // Never set brightness to 0, which can totally turn screen backlight off
                    // and also mess up the calculation due to squaring (and square rooting)
                    val flooredBrightness = calculatedBrightness.coerceAtLeast(BRIGHTNESS_FLOOR)

                    // For gamma correction, the new value is squared
                    val newBrightness = flooredBrightness * flooredBrightness

                    // When setting the progress, we want to still have a linear progress display
                    // so we don't use the gamma corrected value
                    onChangeScreenBrightness(newBrightness, calculatedBrightness)
                }
            }
        }
    }

    fun onEndScreenBrightnessChange() {
        screenBrightnessChangeStart = null
        screenBrightnessAtChangeStart = null
    }

    private fun calculateNormalizedScreenPosition(y: Float, viewHeight: Int) =
        (1f.minus(((y / viewHeight) * 1000f).roundToInt() / 1000f) * 1000f).roundToInt() / 1000f

    private fun isSmallMove(start: Float, y: Float, viewHeight: Int): Boolean {
        val distance = abs(start - y)
        val threshold = viewHeight / CHANGE_THRESHOLD_DIVISOR
        return distance < threshold
    }

    companion object {

        private const val CHANGE_THRESHOLD_DIVISOR = 160f

        private const val BRIGHTNESS_FLOOR = 0.01f

    }

}
