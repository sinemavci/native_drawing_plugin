package com.kotlin.sensor_drawing_plugin.coordinate

/**
 * Point in a geodetic spatial reference DecimalDegrees
 *
 * @param latitude  the y-coordinate is the latitude (north or south) [-90, 90] (in degrees) y-axis
 * @param longitude the x-coordinate is the longitude (east or west) [-180, 180] (in degrees) x-axis
 * @param altitude  -6,356,752: approximate radius of the earth (the WGS 84 datum semi-minor axis) [-6,356,752, 55,000,000] (in meters) z-axis
 * */
class Coordinate(
    var latitude: Double, var longitude: Double, var altitude: Double,
) {
    constructor(latitude: Double, longitude: Double) : this(latitude, longitude, 0.0) {
        this.latitude = latitude
        this.longitude = longitude
    }
}
