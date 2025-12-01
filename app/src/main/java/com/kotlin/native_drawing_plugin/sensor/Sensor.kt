package com.kotlin.native_drawing_plugin.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Priority
import com.kotlin.native_drawing_plugin.DrawingManager
import com.kotlin.native_drawing_plugin.ServiceLocator
import com.kotlin.native_drawing_plugin.coordinate.Coordinate
import com.kotlin.native_drawing_plugin.coordinate.Location
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class Sensor {
    var id = UUID.randomUUID().mostSignificantBits.toString()
    val locationChangedFlow = MutableSharedFlow<Location>()

    val bearingChangedFlow = MutableSharedFlow<Double>()
    val statusChangedFlow = MutableSharedFlow<SensorStatus>()

    private val locationManager: LocationManager =
        ServiceLocator.sensorContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun start() = withContext(Dispatchers.Main.immediate) {
//    val mainLooper = Looper.getMainLooper()
//        Handler(mainLooper).post {
            try {
                if (checkLocationPermission()) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            LOCATION_REQUEST_INTERVAL,
                            LOCATION_REQUEST_DISTANCE,
                            locationListener
                        ) // listener
                    }

                   else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            LOCATION_REQUEST_INTERVAL,
                            LOCATION_REQUEST_DISTANCE,
                            locationListener
                        )
                    }
                    else {

                        // as a backup
                        locationManager.requestLocationUpdates(
                            LocationManager.PASSIVE_PROVIDER,
                            LOCATION_REQUEST_INTERVAL,
                            LOCATION_REQUEST_DISTANCE,
                            locationListener
                        )
                    }
                     statusChangedFlow.emit(SensorStatus.STARTED)
                } else {
                      statusChangedFlow.emit(SensorStatus.PERMISSION_NOT_FOUND)
                }
            } catch (e: Exception) {
                throw Exception("An error occurred while starting location source: ${e.message}")
            }
//        }
    }

    suspend fun stop() {
        try {
            locationManager.removeUpdates(locationListener)
            statusChangedFlow.emit(SensorStatus.STOPPED)
        } catch (e: Exception) {
            throw Exception("An error occurred while stopping location source: ${e.message}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val locationListener = LocationListener { location ->
        var lastGpsLocation: android.location.Location? = null
        var lastNetworkLocation: android.location.Location? = null
        var lastPassiveLocation: android.location.Location? = null

        when (location.provider) {
            LocationManager.GPS_PROVIDER -> lastGpsLocation = location
            LocationManager.NETWORK_PROVIDER -> lastNetworkLocation = location
            LocationManager.PASSIVE_PROVIDER -> lastPassiveLocation = location
        }

        val mostAccurateLocation = arrayOf(lastGpsLocation, lastNetworkLocation, lastPassiveLocation)
                .filterNotNull().minByOrNull { it.accuracy }

        if (mostAccurateLocation != null) {
            val coordinate = Coordinate(
                mostAccurateLocation.latitude,
                mostAccurateLocation.longitude,
                mostAccurateLocation.altitude
            )
            if(DrawingManager.pathView != null) {
                DrawingManager.pathView!!.addGeoPoint(location.latitude, location.longitude)
            }

            ServiceLocator.scope.launch(Dispatchers.Main) {
                bearingChangedFlow.emit(mostAccurateLocation.bearing.toDouble())
                locationChangedFlow.emit(
                    Location(
                        coordinate = coordinate,
                        bearing = mostAccurateLocation.bearing.toDouble(),
                        speed = mostAccurateLocation.speed.toDouble(),
                        time = mostAccurateLocation.time,
                    )
                )
            }

        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            ServiceLocator.sensorContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    ServiceLocator.sensorContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    internal companion object Companion {
        const val LOCATION_REQUEST_PRIORITY: Int = Priority.PRIORITY_HIGH_ACCURACY
        const val LOCATION_REQUEST_INTERVAL: Long = 1000 // milliseconds
        const val LOCATION_REQUEST_DISTANCE: Float = 10f // meters
    }
}