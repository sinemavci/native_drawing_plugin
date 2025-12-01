package com.kotlin.native_drawing_plugin.coordinate

class Location(
    var coordinate: Coordinate,
    var bearing: Double? = null,
    var speed: Double? = 0.0,
    var time: Long = System.currentTimeMillis(),
) {
    constructor(coordinate: Coordinate) : this(coordinate, null, 0.0, System.currentTimeMillis())
}