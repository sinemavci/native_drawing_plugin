package com.kotlin.native_drawing_plugin.tool

import android.graphics.Paint
import com.kotlin.native_drawing_plugin.PaintMode

class MarkerTool : IPaintTool {
    override var paintMode: PaintMode = PaintMode.ERASER

    override fun onStart() {
        TODO("Not yet implemented")
    }

    override fun onMove() {
        TODO("Not yet implemented")
    }

    override fun onEnd() {
        TODO("Not yet implemented")
    }

    override fun createPaint(paint: Paint, color: Int): Paint {
        return paint.apply {
            setColor(color)
            style = Paint.Style.FILL_AND_STROKE
        }
    }
}