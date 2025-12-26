package com.kotlin.native_drawing_plugin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale

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

    override fun import(bitmap: Bitmap) {
        val normalizedBitmap = normalizeBitmap(bitmap, maxSize = 2048)
        paintBoxView.import(normalizedBitmap)
    }

    override fun export(path: String, mimeType: String, fileName: String?) {
      return paintBoxView.export(path, mimeType, fileName)
    }

    override fun isEnable(): Boolean {
        return paintBoxView.isEnable()
    }

    override fun setEnable(isEnable: Boolean) {
        paintBoxView.setEnable(isEnable)
    }

}