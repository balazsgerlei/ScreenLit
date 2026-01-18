package dev.gerlot.screenlit.util

import kotlin.math.abs
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
            screenBrightnessAtChangeStart = Math.round(linearizedBrightness * 1000f) / 1000f
        }
    }

    fun onScreenBrightnessChangeRequest(viewHeight: Int, y: Float, onChangeScreenBrightness: (newBrightness: Float) -> Unit) {
        screenBrightnessChangeStart?.let { start ->
            val normalizedStart = calculateNormalizedScreenPosition(start, viewHeight)
            if (!isSmallMove(start, y, viewHeight)) { // Ignore small movement that can be an imprecise tap
                val normalizedScreenPosition = calculateNormalizedScreenPosition(y, viewHeight)
                val screenBrightnessChange = Math.round((normalizedScreenPosition - normalizedStart) * 1000f) / 1000f

                val previousBrightness = screenBrightnessAtChangeStart
                previousBrightness?.let {
                    val newBrightness = (Math.round((it + screenBrightnessChange) * 1000f) / 1000f).coerceIn(0f, 1f)

                    // For gamma correction, the new value is squared
                    onChangeScreenBrightness(newBrightness * newBrightness)
                }
            }
        }
    }

    fun onEndScreenBrightnessChange() {
        screenBrightnessChangeStart = null
        screenBrightnessAtChangeStart = null
    }

    private fun calculateNormalizedScreenPosition(y: Float, viewHeight: Int) = Math.round(1f.minus(Math.round((y / viewHeight) * 1000f) / 1000f) * 1000f) / 1000f

    private fun isSmallMove(start: Float, y: Float, viewHeight: Int): Boolean {
        val distance = abs(start - y)
        val threshold = viewHeight / CHANGE_THRESHOLD_DIVISOR
        return distance < threshold
    }

    companion object {

        private const val CHANGE_THRESHOLD_DIVISOR = 160f

    }

}
