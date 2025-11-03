package com.kotlin.sensor_drawing_plugin

import android.Manifest
import androidx.annotation.RequiresPermission
import com.kotlin.sensor_drawing_plugin.datasource.LocationDataSource
import kotlinx.coroutines.launch

class SensorManager {
    private var datasourceList: MutableList<LocationDataSource> = mutableListOf()

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun addSensor(dataSource: LocationDataSource) {
        datasourceList.add(dataSource)
        //todo
        ServiceLocator.scope.launch {
            dataSource.start()
        }
    }

    fun removeSensor(dataSource: LocationDataSource) {
        datasourceList.remove(dataSource)
        //todo
        ServiceLocator.scope.launch {
            dataSource.stop()
        }
    }

    fun findSensor(dataSourceId: String): LocationDataSource? {
        return datasourceList.find { it.id == dataSourceId }
    }

}