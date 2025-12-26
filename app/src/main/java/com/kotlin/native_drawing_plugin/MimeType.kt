package com.kotlin.native_drawing_plugin

import android.util.Log

enum class MimeType(val extension: String) {
    PNG("png"),
    JPEG("jpeg"),
    BMP("bmp"),
    PDF("pdf"),
    GIF("gif"),
    TIFF("tif");

    companion object {
        fun fromValue(value: String): MimeType {
            Log.e("from value", "from value: $value")
            return MimeType.entries.first { it.extension == value }
        }
    }
}