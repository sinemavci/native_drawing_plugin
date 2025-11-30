package com.kotlin.sensor_drawing_plugin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver

internal class CustomPainterView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val path = Path()

    // reference origin (first point)
    private var refLat: Double? = null
    private var refLon: Double? = null

    // meter-space bounding box (relative to ref)
    private var minXm = Double.POSITIVE_INFINITY
    private var maxXm = Double.NEGATIVE_INFINITY
    private var minYm = Double.POSITIVE_INFINITY
    private var maxYm = Double.NEGATIVE_INFINITY

    // store raw meter coords (so we can recompute path when scale/box changes)
    private val pointsMeters = mutableListOf<Pair<Double, Double>>() // (xm, ym)

    // pixels-per-meter scale (computed when we know canvas size and bounds)
    private var scale = 1.0

    private var canvasWidth = 0
    private var canvasHeight = 0
    private val padding = 120f

    init {
        // ensure we get real size even when added programmatically
        viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                canvasWidth = width
                canvasHeight = height
                computeScale() // recompute when size ready
            }
        })
    }

    /**
     * Add a new geolocation point (latitude, longitude)
     * This method:
     *  - sets reference if first point
     *  - converts to meters relative to ref
     *  - updates meters bounding box
     *  - recomputes scale
     *  - rebuilds path and invalidates
     */
    fun addGeoPoint(lat: Double, lon: Double) {
        if (refLat == null || refLon == null) {
            refLat = lat
            refLon = lon
        }

        val (xm, ym) = latLonToMeters(lat, lon, refLat!!, refLon!!)

        // add to list
        pointsMeters.add(xm to ym)

        // update bounds in meters
        if (xm < minXm) minXm = xm
        if (xm > maxXm) maxXm = xm
        if (ym < minYm) minYm = ym
        if (ym > maxYm) maxYm = ym

        computeScale()
        rebuildPath()
        invalidate()
    }

    // convert lat/lon (deg) to approximate meters relative to reference point
    private fun latLonToMeters(lat: Double, lon: Double, refLat: Double, refLon: Double): Pair<Double, Double> {
        // meters per degree approximations
        val metersPerDegLat = 110574.0
        val metersPerDegLon = 111320.0 * kotlin.math.cos(Math.toRadians(refLat))
        val deltaLat = lat - refLat
        val deltaLon = lon - refLon
        val ym = deltaLat * metersPerDegLat      // north positive
        val xm = deltaLon * metersPerDegLon      // east positive
        return Pair(xm, ym)
    }

    // compute scale by bounding box in meters and available canvas px
    private fun computeScale() {
        if (canvasWidth == 0 || canvasHeight == 0) return
        val widthMeters = if (minXm.isInfinite() || maxXm.isInfinite()) 0.0 else (maxXm - minXm)
        val heightMeters = if (minYm.isInfinite() || maxYm.isInfinite()) 0.0 else (maxYm - minYm)

        if (widthMeters == 0.0 && heightMeters == 0.0) {
            scale = 1.0
            return
        }

        val availW = (canvasWidth - 2 * padding).coerceAtLeast(1f).toDouble()
        val availH = (canvasHeight - 2 * padding).coerceAtLeast(1f).toDouble()

        val sx = if (widthMeters > 0.0) (availW / widthMeters) else Double.POSITIVE_INFINITY
        val sy = if (heightMeters > 0.0) (availH / heightMeters) else Double.POSITIVE_INFINITY

        scale = kotlin.math.min(sx, sy).takeIf { it.isFinite() } ?: 1.0
        scale = scale.coerceIn(0.5, 5.0)
    }

    // Rebuild Android Path from pointsMeters using current scale and canvas center
    private fun rebuildPath() {
        path.reset()
        if (pointsMeters.isEmpty()) return

        // center mapping: put bounding box centered or align left/top as you wish
        // We'll place the bounding box with padding from left/top:
        val offsetLeftPx = padding
        val offsetTopPx = padding

        // Option: map minXm -> offsetLeftPx, maxXm -> offsetLeftPx + width_px
        // Compute screen X for a meter X: screenX = offsetLeftPx + (xm - minXm) * scale
        // For Y: screenY = offsetTopPx + (maxYm - ym) * scale  (invert Y so north up)

        for ((index, pair) in pointsMeters.withIndex()) {
            val (xm, ym) = pair
            val screenX = (offsetLeftPx + (xm - minXm) * scale).toFloat()
            val screenY = (offsetTopPx + (maxYm - ym) * scale).toFloat() // invert Y
            if (index == 0) path.moveTo(screenX, screenY) else path.lineTo(screenX, screenY)
        }
    }


    fun addPoint(point: PointF) {
        if (path.isEmpty) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Log.e("sinem", "onsizechanged w:${w} h:${h}")
        super.onSizeChanged(w, h, oldw, oldh)
        canvasWidth = w
        canvasHeight = h
        computeScale()
        rebuildPath()
    }
}