package dev.gerlot.screenlit.extension

import android.app.Activity
import dev.gerlot.systembarcolorist.SystemBarColorist

fun Activity.setSystemBarBackgrounds(statusBarColor: Int, navigationBarColor: Int) {
    SystemBarColorist.colorSystemBarsOfWindow(window, statusBarColor, navigationBarColor)
}
