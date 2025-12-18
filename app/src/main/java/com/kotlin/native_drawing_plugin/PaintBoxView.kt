package com.kotlin.native_drawing_plugin

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import kotlin.collections.mutableListOf
import kotlin.math.abs

class PaintBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    val paintEditor = PaintEditor(paintBoxView = this)

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

    fun export(): Bitmap? {
        return extraBitmap?.copy(extraBitmap!!.config!!, false)
    }

    fun import(bitmap: Bitmap) {
        // Clear history
        strokes.clear()
        undoStrokes.clear()

        // Recreate offscreen buffer if needed
        if (extraBitmap == null ||
            extraBitmap?.width != bitmap.width ||
            extraBitmap?.height != bitmap.height
        ) {
            extraBitmap?.recycle()
            extraBitmap = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )
            extraCanvas = Canvas(extraBitmap!!)
        }

        // Draw imported bitmap
        extraCanvas?.drawColor(Color.WHITE)
        extraCanvas?.drawBitmap(bitmap, 0f, 0f, null)

        redrawSurface()
    }
}