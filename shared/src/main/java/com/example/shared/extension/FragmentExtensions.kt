package com.example.shared.extension

import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.fragment.app.Fragment

fun Fragment.setSoftInputModeResize() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity?.window?.setDecorFitsSystemWindows(false)

        view?.setOnApplyWindowInsetsListener { content, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
            content.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, imeHeight))

            windowInsets
        }
        view?.requestApplyInsets()
    } else {
        @Suppress("DEPRECATION")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}

fun Fragment.resetSoftInputMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity?.window?.setDecorFitsSystemWindows(true)
        view?.setOnApplyWindowInsetsListener(null)
    } else {
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }
}