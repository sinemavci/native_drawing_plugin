package com.kotlin.native_drawing_plugin.export_util

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.get
import org.beyka.tiffbitmapfactory.CompressionScheme
import org.beyka.tiffbitmapfactory.TiffSaver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class ExportUtil {
    fun createGifFromBitmap(gifFrames: List<Bitmap>, saveDirectoryPath: String, saveFilePath: String) {
        val hiddenTempPath = "$saveDirectoryPath.temp_$saveFilePath"
        val finalPath = "$saveDirectoryPath$saveFilePath"

        try {
            val outStream = FileOutputStream(hiddenTempPath)
            val bos = ByteArrayOutputStream()
            val encoder = GifEncoder()
            encoder.setDelay(1000)
            encoder.start(bos)
            gifFrames.forEach { frame ->
                encoder.addFrame(frame)
            }
            encoder.finish()
            outStream.write(bos.toByteArray())
            outStream.close()
            val hiddenTempFile = File(hiddenTempPath)
            val finalFile = File(finalPath)

            if (hiddenTempFile.exists()) {
                hiddenTempFile.renameTo(finalFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createPdfFromBitmap(bitmap: Bitmap, savePath: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val pdfFile = File(savePath)
        try {
            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            println("PDF saved successfully at: ${pdfFile.absolutePath}")
        } catch (e: IOException) {
            println("Error saving PDF: ${e.message}")
        } finally {
            pdfDocument.close()
        }
    }

    private fun writeInt(array: ByteArray, offset: Int, value: Int) {
        if (offset + 3 < array.size) {
            array[offset] = (value and 0xFF).toByte()
            array[offset + 1] = ((value shr 8) and 0xFF).toByte()
            array[offset + 2] = ((value shr 16) and 0xFF).toByte()
            array[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
    }

    private fun writeShort(array: ByteArray, offset: Int, value: Short) {
        if (offset + 1 < array.size) {
            array[offset] = (value.toInt() and 0xFF).toByte()
            array[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }
    }

    fun createBmpFromBitmap(bitmap: Bitmap, savePath: String): File {
        val file = File(savePath)
        try {
            val width = bitmap.width
            val height = bitmap.height
            val rowSize = ((width * 3 + 3) / 4) * 4 // Row size must be a multiple of 4 bytes
            val pixelDataSize = rowSize * height

            // Create ByteArrayOutputStream to write BMP data
            val outputStream = ByteArrayOutputStream()

            // Bitmap File Header (14 bytes)
            val fileHeader = ByteArray(14)
            fileHeader[0] = 'B'.code.toByte()
            fileHeader[1] = 'M'.code.toByte()
            writeInt(fileHeader, 2, fileHeader.size + pixelDataSize) // File size
            writeShort(fileHeader, 10, 54) // Offset to pixel data
            outputStream.write(fileHeader)

            val infoHeader = ByteArray(40)
            writeInt(infoHeader, 0, 40) // Header size
            writeInt(infoHeader, 4, width) // Image width
            writeInt(infoHeader, 8, height) // Image height
            writeShort(infoHeader, 12, 1) // Number of color planes
            writeShort(infoHeader, 14, 24) // Bits per pixel
            writeInt(infoHeader, 16, 0) // Compression method (0 = none)
            writeInt(infoHeader, 20, pixelDataSize) // Image size
            writeInt(infoHeader, 24, 2835) // Horizontal resolution (72 DPI)
            writeInt(infoHeader, 28, 2835) // Vertical resolution (72 DPI)
            writeInt(infoHeader, 32, 0) // Number of colors in palette
            writeInt(infoHeader, 36, 0) // Important colors
            outputStream.write(infoHeader)

            // Write pixel data (bottom to top)
            val pixelData = ByteArray(rowSize * height)
            for (y in height - 1 downTo 0) {
                val rowOffset = y * rowSize
                for (x in 0 until width) {
                    val pixel = bitmap[x, height - y - 1]
                    pixelData[rowOffset + x * 3] = (pixel and 0xFF).toByte() // Blue
                    pixelData[rowOffset + x * 3 + 1] =
                        ((pixel shr 8) and 0xFF).toByte() // Green
                    pixelData[rowOffset + x * 3 + 2] = ((pixel shr 16) and 0xFF).toByte() // Red
                }
            }
            outputStream.write(pixelData)

            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.use { fos ->
                fos.write(outputStream.toByteArray())
            }
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    fun createTiffFromBitmap(bitmap: Bitmap, savePath: String) {
        val options = TiffSaver.SaveOptions()
        options.compressionScheme = CompressionScheme.JPEG
        TiffSaver.saveBitmap(savePath, bitmap, options)
    }
}