package com.kotlin.native_drawing_plugin

import android.graphics.Bitmap

abstract class IPaintEditor {
    open fun redo() {}
    open fun undo() {}
    open fun reset() {}

    open fun import(bitmap: Bitmap) {}

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

}