package com.kotlin.sensor_drawing_plugin

import android.Manifest
import androidx.annotation.RequiresPermission
import com.kotlin.sensor_drawing_plugin.sensor.Sensor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

class SensorManager {
    private var _sensor: Sensor? = null

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startActivity() {
        //todo
        ServiceLocator.scope.launch {
            _sensor?.start()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun stopActivity() {
        _sensor = null
        //todo
        ServiceLocator.scope.launch {
            _sensor?.stop()
        }
    }

    fun setSensor(sensor: Sensor) {
        _sensor = sensor
    }

    fun getSensor(): Sensor? {
        return _sensor
    }
}