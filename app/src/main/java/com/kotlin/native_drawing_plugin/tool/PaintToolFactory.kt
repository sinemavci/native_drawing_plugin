package com.kotlin.native_drawing_plugin.tool

import com.kotlin.native_drawing_plugin.PaintMode

object PaintToolFactory {
    fun create(mode: PaintMode): IPaintTool {
        return when(mode) {
            PaintMode.PEN -> PenTool()
            PaintMode.BRUSH -> PenTool()
            PaintMode.BUCKET -> BucketTool()
            PaintMode.ERASER -> EraserTool()
            PaintMode.MARKER -> MarkerTool()
        }
    }
}