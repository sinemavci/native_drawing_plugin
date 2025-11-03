package com.kotlin.sensor_drawing_plugin.datasource

enum class LocationDataSourceStatus(value: String) {
    INIT("INIT"),
    STARTED("STARTED"),
    STOPPED("STOPPED"),
    FAILED_TO_START("FAILED_TO_START"),
    PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND");

    companion object {
        fun fromValue(value: String): LocationDataSourceStatus {
            return LocationDataSourceStatus.entries.first { it.name == value }
        }
    }
}