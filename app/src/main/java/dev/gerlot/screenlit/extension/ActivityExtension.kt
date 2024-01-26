package dev.gerlot.screenlit.extension

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import dev.gerlot.screenlit.util.ColorDarkness
import dev.gerlot.screenlit.util.calculateColorDarkness

fun Activity.setSystemBarBackgrounds(statusBarColor: Int, navigationBarColor: Int) {
    val brightStatusBarColor = calculateColorDarkness(statusBarColor) == ColorDarkness.BRIGHT
    val brightNavigationBarColor = calculateColorDarkness(navigationBarColor) == ColorDarkness.BRIGHT

    window.statusBarColor = statusBarColor
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window.navigationBarColor = navigationBarColor
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let {
            val statusBarAppearance = if(brightStatusBarColor) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0
            val navigationBarAppearance = if(brightNavigationBarColor) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0
            it.setSystemBarsAppearance(statusBarAppearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            it.setSystemBarsAppearance(navigationBarAppearance, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        }
    } else {
        val systemUiVisibility = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (brightStatusBarColor && brightNavigationBarColor) {
                @Suppress("DEPRECATION")
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else if (!brightStatusBarColor && !brightNavigationBarColor) {
                @Suppress("DEPRECATION")
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else if (!brightStatusBarColor && brightNavigationBarColor) {
                @Suppress("DEPRECATION")
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                @Suppress("DEPRECATION")
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        } else {
            if (brightStatusBarColor) {
                @Suppress("DEPRECATION")
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                @Suppress("DEPRECATION")
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }
}
