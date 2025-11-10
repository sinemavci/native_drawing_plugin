package com.kotlin.sensor_drawing_plugin.sensor

enum class SensorStatus(value: String) {
    INIT("INIT"),
    STARTED("STARTED"),
    STOPPED("STOPPED"),
    FAILED_TO_START("FAILED_TO_START"),
    PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND");

    companion object Companion {
        fun fromValue(value: String): SensorStatus {
            return SensorStatus.entries.first { it.name == value }
        }
    }
}