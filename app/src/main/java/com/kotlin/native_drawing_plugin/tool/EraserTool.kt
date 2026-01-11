package com.kotlin.native_drawing_plugin.tool

import android.graphics.Paint
import com.kotlin.native_drawing_plugin.PaintMode

class EraserTool : IPaintTool {
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

    override fun createPaint(paint: Paint): Paint {
        return paint.apply {
            color = android.graphics.Color.WHITE
        }
    }
}