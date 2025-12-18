package com.kotlin.native_drawing_plugin

import android.os.Build
import androidx.annotation.RequiresApi

class PaintEditor(
    private val paintBoxView: PaintBoxView,
) : IPaintEditor() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun undo() {
        paintBoxView.undo()
    }

    override fun redo() {
       paintBoxView.redo()
    }

    override fun reset() {
        paintBoxView.reset()
    }
}