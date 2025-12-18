package com.kotlin.native_drawing_plugin

import android.graphics.Bitmap

abstract class IPaintEditor {
    open fun redo() {}
    open fun undo() {}
    open fun reset() {}

    open fun import(bitmap: Bitmap) {}

    open val isCanRedo: Boolean = false

    open val isCanUndo: Boolean = false

}