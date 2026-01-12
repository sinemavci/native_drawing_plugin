package com.kotlin.native_drawing_plugin.tool

import android.graphics.Paint
import com.kotlin.native_drawing_plugin.PaintMode

interface IPaintTool {
    var paintMode: PaintMode

    fun onStart()

    fun onMove()

    fun onEnd()

    fun createPaint(paint: Paint, color: Int): Paint
}