package com.kotlin.native_drawing_plugin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import kotlin.collections.mutableListOf
import kotlin.math.abs
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import com.kotlin.native_drawing_plugin.export_util.GifEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PaintBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    val paintEditor = PaintEditor(paintBoxView = this)
    private val gifFrames = mutableListOf<Bitmap>()

    private var isPaintBoxViewEnable = true

    // Default paint
    private val paintDefaults = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }

    private data class Stroke(val path: Path, val paint: Paint)

    // All completed strokes
    private val strokes = mutableListOf<Stroke>()

    // Current drawing path
    private var currentPath = Path()
    private var currentPaint = Paint(paintDefaults)

    // Offscreen cache
    private var extraBitmap: Bitmap? = null
    private var extraCanvas: Canvas? = null

    private var undoStrokes = mutableListOf<Stroke>()

    // Touch smoothing
    private var lastX = 0f
    private var lastY = 0f
    private val touchTolerance = 4f

    init {
        holder.addCallback(this)
    }

    // -------------------------------------------------------------------------
    // Surface lifecycle
    // -------------------------------------------------------------------------
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.e("PaintBoxView", "surfaceCreated")

        // Initialize offscreen bitmap based on actual surface size
        val w = width
        val h = height

        if (w > 0 && h > 0) {
            extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            extraCanvas = Canvas(extraBitmap!!)
            extraCanvas?.drawColor(Color.WHITE)
        }

        redrawSurface()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.e("PaintBoxView", "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.e("PaintBoxView", "surfaceDestroyed")
        extraBitmap?.recycle()
        extraBitmap = null
        extraCanvas = null
    }

    // -------------------------------------------------------------------------
    // TOUCH HANDLING
    // -------------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(!isPaintBoxViewEnable) return false
        val pointerIndex = event.actionIndex
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val pressure = event.getPressure(pointerIndex)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                startStroke(x, y, pressure)
                redrawSurface()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                moveStroke(x, y)
                redrawSurface()
                return true
            }

            MotionEvent.ACTION_UP -> {
                endStroke()
                redrawSurface()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // -------------------------------------------------------------------------
    // STROKE HELPERS
    // -------------------------------------------------------------------------

    private fun startStroke(x: Float, y: Float, pressure: Float) {
        currentPath = Path()
        currentPath.moveTo(x, y)
        currentPaint = Paint(paintDefaults)
        currentPaint.strokeWidth = paintDefaults.strokeWidth * pressure

        lastX = x
        lastY = y
    }

    private fun moveStroke(x: Float, y: Float) {
        val dx = abs(x - lastX)
        val dy = abs(y - lastY)

        if (dx >= touchTolerance || dy >= touchTolerance) {
            currentPath.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f)
            lastX = x
            lastY = y
        }
    }

    private fun endStroke() {
        // Save stroke
        val savedPath = Path(currentPath)
        val savedPaint = Paint(currentPaint)
        strokes.add(Stroke(savedPath, savedPaint))

        // Draw to cached bitmap
        extraCanvas?.drawPath(savedPath, savedPaint)
        extraBitmap?.let {
            gifFrames.add(it.copy(it.config!!, false))
        }
        currentPath.reset()
    }

    // -------------------------------------------------------------------------
    // DRAWING TO SURFACE
    // -------------------------------------------------------------------------

    private fun redrawSurface() {
        val canvas = holder.lockCanvas() ?: return

        // Clear surface
        canvas.drawColor(Color.WHITE)

        // Draw cached strokes
        extraBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // Draw current active stroke
        canvas.drawPath(currentPath, currentPaint)

        holder.unlockCanvasAndPost(canvas)
    }

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    fun clear() {
        strokes.clear()
        extraCanvas?.drawColor(Color.WHITE)
        currentPath.reset()
        redrawSurface()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun undo() {
        if (strokes.isNotEmpty()) {
            val stroke = strokes[strokes.size - 1]
            undoStrokes.addFirst(stroke)
            strokes.remove(stroke)
            redrawCachedBitmap()
            redrawSurface()
        }
    }

    internal fun redo() {
        if(undoStrokes.isNotEmpty()) {
            strokes.add(undoStrokes[0])
            undoStrokes.removeAt(0)
            redrawCachedBitmap()
            redrawSurface()
        }
    }

    internal fun reset() {
        if (strokes.isNotEmpty()) {
            strokes.clear()
            undoStrokes.clear()
            redrawCachedBitmap()
            redrawSurface()
        }
    }

    private fun redrawCachedBitmap() {
        extraCanvas?.drawColor(Color.WHITE)
        for (s in strokes) {
            extraCanvas?.drawPath(s.path, s.paint)
        }
    }

    fun setStrokeColor(color: Int) {
        paintDefaults.color = color
    }

    fun setStrokeWidth(widthPx: Float) {
        paintDefaults.strokeWidth = widthPx
    }

    private fun generateGIF(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        val encoder = GifEncoder()
        encoder.setDelay(1000)
        encoder.start(bos)
        gifFrames.forEach { frame ->
            encoder.addFrame(frame)
        }
        encoder.finish()
        return bos.toByteArray()
    }


    fun createGifFromBitmap(bitmap: Bitmap, saveDirectoryPath: String, saveFilePath: String) {
        val hiddenTempPath = "$saveDirectoryPath.temp_$saveFilePath"
        val finalPath = "$saveDirectoryPath$saveFilePath"

        try {
            val outStream = FileOutputStream(hiddenTempPath)
            outStream.write(generateGIF(bitmap))
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

    @SuppressLint("WrongThread")
    private fun bitmapToFile(
        bitmap: Bitmap?,
        path: String,
        mimeType: MimeType,
        fileName: String? = "image_${System.currentTimeMillis()}",
    ) {
//        val dir = File(path, "images")
//        if (!dir.exists()) dir.mkdirs()
        val file = File(path)
        if (!(file.exists() && file.isDirectory)) {
            return
            //throw ScreenShotException("Directory '$path' not found!")
        }

        val normalizedPath = path.replace("\\", "/").trimEnd('/')
        val normalizedFileName = fileName?.replace("\\", "/")?.trimStart('/')
        val saveDirectoryPath = "$normalizedPath/"
        val saveFilePath = "$normalizedFileName.${mimeType.extension}"

        when (mimeType) {
            MimeType.PNG -> {
                FileOutputStream("$saveDirectoryPath$saveFilePath").use { outputStream ->
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Log.e("PaintEditorController", "Image saved to $saveDirectoryPath$saveFilePath")
            }
            MimeType.JPEG ->  {
                FileOutputStream("$saveDirectoryPath$saveFilePath").use { outputStream ->
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Log.e("PaintEditorController", "Image saved to $saveDirectoryPath$saveFilePath")
            }
            MimeType.BMP -> {
                if(bitmap != null) {
                    createBmpFromBitmap(bitmap, "$saveDirectoryPath$saveFilePath")
                    Log.e("PaintEditorController", "Image saved to $saveDirectoryPath$saveFilePath")
                }
            }
            MimeType.PDF ->  {
                if(bitmap != null) {
                    createPdfFromBitmap(bitmap, "$saveDirectoryPath$saveFilePath")
                    Log.e("PaintEditorController", "Image saved to $saveDirectoryPath$saveFilePath")
                }
            }
            MimeType.GIF -> {
                if(bitmap != null) {
                    createGifFromBitmap(bitmap, saveDirectoryPath, saveFilePath)
                    Log.e("PaintEditorController", "Image saved to $saveDirectoryPath$saveFilePath")
                }
            }
            MimeType.TIFF -> TODO()
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

    fun writeInt(array: ByteArray, offset: Int, value: Int) {
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

    fun export(path: String, mimeType: MimeType, fileName: String?) {
        val bitmap = extraBitmap?.copy(extraBitmap!!.config!!, false)
        bitmapToFile(bitmap, path, mimeType, fileName)
    }

    fun import(bitmap: Bitmap) {
        if(!isPaintBoxViewEnable) return
        // Clear history
//        strokes.clear()
//        undoStrokes.clear()

        // Recreate offscreen buffer if needed
        if (extraBitmap == null ||
            extraBitmap?.width != bitmap.width ||
            extraBitmap?.height != bitmap.height
        ) {
            extraBitmap?.recycle()
            extraBitmap = createBitmap(bitmap.width, bitmap.height)
            extraCanvas = Canvas(extraBitmap!!)
        }

        // Draw imported bitmap
        extraCanvas?.drawColor(Color.WHITE)
        extraCanvas?.drawBitmap(bitmap, 0f, 0f, null)

        redrawSurface()
    }

    fun setEnable(isEnable: Boolean) {
        Log.e("setEnable sdk", "setEnable sdk: ${isEnable}")
        isPaintBoxViewEnable = isEnable
    }

    fun isEnable(): Boolean {
        Log.e("isEnable sdk", "isEnable sdk: ${isPaintBoxViewEnable}")
        return isPaintBoxViewEnable
    }
}