package com.kotlin.native_drawing_plugin

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.toColor

abstract class IPaintEditor {
    open fun redo() {}
    open fun undo() {}
    open fun reset() {}

    open fun import(path: String, width: Double?, height: Double?) {}

    open fun export(path: String, mimeType: MimeType, fileName: String?) {}

    open val isCanRedo: Boolean = false

    open val isCanUndo: Boolean = false

    open fun setEnable(isEnable: Boolean) {}

    open fun isEnable(): Boolean {
        return true
    }

    open fun setPaintMode(paintMode: PaintMode) {}

    open fun getPaintMode(): PaintMode {
        return PaintMode.PEN
    }

    open fun setStrokeColor(color: Color) {}

    open fun getStrokeColor(): Color {
        return Color.BLACK.toColor()
    }

    open fun setStrokeWidth(strokeWidth: Double) {}

    open fun getStrokeWidth(): Double {
        return 12.0
    }

}