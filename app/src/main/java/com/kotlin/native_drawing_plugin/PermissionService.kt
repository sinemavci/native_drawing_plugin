package com.kotlin.native_drawing_plugin

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionService {
    private var pendingCallback: ((Boolean) -> Unit)? = null

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            ServiceLocator.sensorContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    ServiceLocator.sensorContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(activity: Activity, callback: (Boolean) -> Unit) {
        pendingCallback = callback
        if (hasLocationPermission()) {
            return callback(true)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                1001
            )
        }
    }

    fun onPermissionResult(grantResults: IntArray): Boolean {
        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                grantResults[1] == PackageManager.PERMISSION_GRANTED
        pendingCallback?.invoke(granted)
        pendingCallback = null
        return granted
    }
}

