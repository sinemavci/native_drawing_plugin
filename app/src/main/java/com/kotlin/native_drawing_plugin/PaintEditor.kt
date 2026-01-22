package com.kotlin.native_drawing_plugin

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi

fun normalizeBitmap(
    source: Bitmap,
    maxSize: Int = 2048,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap {

    val maxDim = maxOf(source.width, source.height)
    if (maxDim <= maxSize && source.config == config && source.isMutable) {
        return source // 🚀 zero extra work
    }

    val scale = minOf(maxSize.toFloat() / maxDim, 1f)

    val width = (source.width * scale).toInt()
    val height = (source.height * scale).toInt()

    val resized =
        if (scale < 1f)
            Bitmap.createScaledBitmap(source, width, height, true)
        else
            source

    return if (resized.config != config || !resized.isMutable)
        resized.copy(config, true)
    else
        resized
}

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

    override fun import(path: String, width: Double?, height: Double?) {
        paintBoxView.import(path, width, height)
    }

    override fun export(path: String, mimeType: MimeType, fileName: String?) {
      return paintBoxView.export(path, mimeType, fileName)
    }

    override fun isEnable(): Boolean {
        return paintBoxView.isEnable()
    }

    override fun setEnable(isEnable: Boolean) {
        paintBoxView.setEnable(isEnable)
    }

    override fun setPaintMode(paintMode: PaintMode) {
        paintBoxView.setPaintMode(paintMode)
    }

    override fun getPaintMode(): PaintMode {
        return paintBoxView.getPaintMode()
    }

    override fun setStrokeColor(color: Color) {
        paintBoxView.setStrokeColor(color)
    }

    override fun getStrokeColor(): Color {
        return paintBoxView.getStrokeColor()
    }

    override fun setStrokeWidth(strokeWidth: Double) {
        paintBoxView.setStrokeWidth(strokeWidth.toFloat())
    }

    override fun getStrokeWidth(): Double {
        return paintBoxView.getStrokeWidth()
    }

}