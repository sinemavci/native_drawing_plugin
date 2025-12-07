package com.kotlin.native_drawing_plugin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class PaintBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint for drawing strokes (default)
    private val paintDefaults = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }

    // Data class to keep path + paint (so each stroke can have own style)
    private data class Stroke(val path: Path, val paint: Paint)

    // Keep completed strokes
    private val strokes = mutableListOf<Stroke>()

    // Current working path
    private var currentPath = Path()
    private var currentPaint = Paint(paintDefaults)

    // Backing bitmap/canvas for faster redraws
    private var extraBitmap: Bitmap? = null
    private var extraCanvas: Canvas? = null

    // For smoothing: last touch coordinates
    private var lastX = 0f
    private var lastY = 0f
    private val touchTolerance = 4f

    // Optional: if you want to detect taps separately
    var onTapListener: ((x: Float, y: Float) -> Unit)? = null

    init {
        Log.e("paintboxview", "paintboxview sdk init")
        // Optionally read attributes from XML (color, stroke width) here
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            extraBitmap?.recycle()
            extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            extraCanvas = Canvas(extraBitmap!!)
            // Fill white background (or transparent if you prefer)
            extraCanvas?.drawColor(Color.WHITE)
            // re-render existing strokes if any
            redrawStrokesOnCanvas()
        }
    }

    override fun onDraw(canvas: Canvas) {
        Log.e("paintboxview", "paintboxview sdk onDraw")
        super.onDraw(canvas)
        // draw the cached bitmap (all previous strokes)
        extraBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        // draw current path on top
        canvas.drawPath(currentPath, currentPaint)
    }

    private fun redrawStrokesOnCanvas() {
        extraCanvas?.drawColor(Color.WHITE) // clear
        for (s in strokes) {
            extraCanvas?.drawPath(s.path, s.paint)
        }
    }

    // Start a new stroke (copy paint so each stroke preserves style)
    private fun startStroke(x: Float, y: Float, pressure: Float = 1f) {
        currentPath = Path()
        currentPath.moveTo(x, y)
        // copy paint to freeze stroke style
        currentPaint = Paint(paintDefaults)
        // optionally vary strokeWidth by pressure:
        currentPaint.strokeWidth = paintDefaults.strokeWidth * pressure
        lastX = x
        lastY = y
    }

    private fun moveStroke(x: Float, y: Float) {
        val dx = abs(x - lastX)
        val dy = abs(y - lastY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // Use quadTo for smoothing
            currentPath.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f)
            lastX = x
            lastY = y
        }
    }

    private fun endStroke() {
        // finish path and push it to strokes and draw onto bitmap canvas for cache
        //currentPath.lineTo(lastX, lastY)
        // store stroke copy
        val savedPath = Path(currentPath)
        val savedPaint = Paint(currentPaint)
        strokes.add(Stroke(savedPath, savedPaint))
        // draw onto backing canvas
        extraCanvas?.drawPath(savedPath, savedPaint)
        // clear current path
        currentPath.reset()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // handle multi-touch by focusing on pointerIndex from event.actionIndex if desired
        val pointerIndex = event.actionIndex
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val pressure = event.getPressure(pointerIndex)

        Log.e("event", "motion eventttttt: ${event.actionMasked}")

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // start stroke
                startStroke(x, y, pressure)
                invalidate()
                // It's also a tap candidate; onTap can be called on ACTION_UP if no movement
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Another finger touched — you can decide to begin a new path or ignore
                // Here we begin a new stroke for that pointer:
                startStroke(x, y, pressure)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // For MOVE with multiple pointers, loop through them if you want full multi-touch drawing.
                // This simple version uses the primary pointer movement:
                moveStroke(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                // If touch ended shortly after ACTION_DOWN and path is tiny, treat as a tap:
                // We can check path bounds or small distance.
                // For simplicity: if stroke had almost no movement -> it's a tap
                // Save stroke and call tap listener
                endStroke()
                invalidate()
                // detect tap: small stroke (distance)
                // Here we check if the stroke was just a dot by verifying stroke bounding box area small:
                val bounds = RectF()
                strokes.lastOrNull()?.path?.computeBounds(bounds, true)
                val isTap = bounds.width() < 12f && bounds.height() < 12f
                if (isTap) {
                    onTapListener?.invoke(x, y)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // Public helpers
    fun clear() {
        strokes.clear()
        extraCanvas?.drawColor(Color.WHITE)
        currentPath.reset()
        invalidate()
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            redrawStrokesOnCanvas()
            invalidate()
        }
    }

    fun setStrokeColor(color: Int) {
        paintDefaults.color = color
    }

    fun setStrokeWidth(widthPx: Float) {
        paintDefaults.strokeWidth = widthPx
    }

    // Export drawing as Bitmap
    fun exportBitmap(): Bitmap? {
        // return copy to avoid exposing internal bitmap
        return extraBitmap?.copy(extraBitmap!!.config!!, false)
    }
}