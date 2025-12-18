package com.kotlin.native_drawing_plugin

abstract class IPaintEditor {
    open fun redo() {}
    open fun undo() {}

    open fun reset() {}

    open val isCanRedo: Boolean = false

    open val isCanUndo: Boolean = false

}