package com.kotlin.native_drawing_plugin

class PaintEditor(
    private val paintBoxView: PaintBoxView,
) : IPaintEditor() {

    override fun undo() {
        paintBoxView.undo()
    }

    override fun redo() {
       paintBoxView.redo()
    }
}