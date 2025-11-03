package com.kotlin.sensor_drawing_plugin

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@SuppressLint("StaticFieldLeak")
internal object ServiceLocator {
    lateinit var sensorContext: Context

    @OptIn(DelicateCoroutinesApi::class)
    lateinit var scope: GlobalScope
}