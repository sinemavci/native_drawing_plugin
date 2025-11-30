package com.kotlin.sensor_drawing_plugin

//noinspection SuspiciousImport
import android.R
import android.app.Activity
import android.view.ViewGroup

object DrawingManager {
    internal var pathView: CustomPainterView? = null

    fun attachDrawingView(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(R.id.content)
        val pathView = CustomPainterView(activity)
        root.addView(pathView)
        this.pathView = pathView
    }

}