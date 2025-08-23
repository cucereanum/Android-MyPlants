package com.example.myplants.presentation.util

object DebounceClick {

    private var lastClickTime = 0L

    fun debounceClick(threshold: Long = 1000L, action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > threshold) {
            lastClickTime = currentTime
            action()
        }
    }

}