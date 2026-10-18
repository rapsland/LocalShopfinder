package com.localshopfinder.app

data class CityLocation(val name: String, val lat: Double, val lon: Double)

data class ShopItem(
    val name: String,
    val brand: String,
    val lat: Double,
    val lon: Double,
    var distanceMeters: Float = 0f
)
