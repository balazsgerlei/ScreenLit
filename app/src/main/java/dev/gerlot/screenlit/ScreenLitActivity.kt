package dev.gerlot.screenlit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.gerlot.screenlit.extension.setSystemBarBackgrounds


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class ScreenLitActivity : AppCompatActivity() {

    private lateinit var fullscreenContent: FrameLayout
    private lateinit var gestureDescriptionTv: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Hide the status bar
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            // Allow showing the status bar with swiping from top to bottom
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        /*if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }*/
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
        gestureDescriptionTv.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    private var isNightVision: Boolean = false

    private var screenBrightnessChangeStart: Float? = null
    private var screenBrightnessAtChangeStart: Float? = null

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        val statusBarColor = ResourcesCompat.getColor(resources, R.color.grey_100, null)
        val navigationBarColor = ResourcesCompat.getColor(resources, R.color.grey_100, null)
        setSystemBarBackgrounds(statusBarColor, navigationBarColor)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = findViewById(R.id.fullscreen_content)
        val gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleNightVisionMode()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggle()
                return true
            }
        })
        fullscreenContent.setOnTouchListener { view, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
            when(motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    screenBrightnessChangeStart = calculateNormalizedScreenPosition(motionEvent.y, view.height)
                    screenBrightnessAtChangeStart = window?.attributes?.let { Math.round(it.screenBrightness * 1000f) / 1000f }
                }
                MotionEvent.ACTION_MOVE -> {
                    screenBrightnessChangeStart?.let {
                        val screenBrightnessChange = Math.round((calculateNormalizedScreenPosition(motionEvent.y, view.height) - it) * 1000f) / 1000f * 2f
                        changeScreenBrightness(screenBrightnessChange)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    screenBrightnessChangeStart = null
                    screenBrightnessAtChangeStart = null
                }
            }
            true
        }

        gestureDescriptionTv = findViewById(R.id.gestureDescriptionTv)

        fullscreenContentControls = findViewById(R.id.fullscreen_content_controls)

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById<Button>(R.id.dummy_button).setOnTouchListener(delayHideTouchListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(INITIAL_AUTO_HIDE_DELAY_MILLIS)
    }

    private fun calculateNormalizedScreenPosition(y: Float, viewHeight: Int) = Math.round(1f.minus(Math.round((y / viewHeight) * 1000f) / 1000f) * 1000f) / 1000f

    private fun changeScreenBrightness(brightnessChange: Float) {
        window?.attributes?.let { layoutParams ->
            val previousBrightness = screenBrightnessAtChangeStart
            previousBrightness?.let {
                layoutParams.screenBrightness = (Math.round((it + brightnessChange) * 1000f) / 1000f).coerceIn(0f, 1f)
                window?.attributes = layoutParams
            }
        }
    }

    private fun toggleNightVisionMode() {
        if (isNightVision) {
            fullscreenContent.setBackgroundColor(Color.WHITE)
            val statusBarColor = ResourcesCompat.getColor(resources, R.color.grey_100, null)
            val navigationBarColor = ResourcesCompat.getColor(resources, R.color.grey_100, null)
            setSystemBarBackgrounds(statusBarColor, navigationBarColor)
            gestureDescriptionTv.setTextColor(ResourcesCompat.getColor(resources, R.color.grey_500, null))
        } else {
            fullscreenContent.setBackgroundColor(Color.RED)
            setSystemBarBackgrounds(Color.RED, Color.RED)
            gestureDescriptionTv.setTextColor(ResourcesCompat.getColor(resources, R.color.grey_100, null))
        }
        isNightVision = !isNightVision
    }

    private fun toggle() {
        hideHandler.removeCallbacks(hideRunnable)
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        gestureDescriptionTv.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY_MILLIS.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY_MILLIS.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY_MILLIS = 300

        private const val INITIAL_AUTO_HIDE_DELAY_MILLIS = 4000

        fun newIntent(context: Context) = Intent(context, ScreenLitActivity::class.java)

    }
}
