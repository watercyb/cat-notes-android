package com.blueberryjoy.notes.utils

import android.view.MotionEvent
import android.view.View

object ClickAnimation {

    fun set(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .alpha(0.7f)
                        .setDuration(50)
                        .start()

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(50)
                        .start()

                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }

                    true
                }

                else -> false
            }
        }
    }
}