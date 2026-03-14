package com.example.colman_android_final_assigment.utils

object Constants {
    // Mapping of Display Name to Government City ID
    val CITIES = mapOf(
        "Tel Aviv" to 5000,
        "Haifa" to 4000,
        "Jerusalem" to 3000,
        "Beersheba" to 9000,
        "Eilat" to 2600
    )

    fun getCityNames() = CITIES.keys.toTypedArray()
    fun getCityIdByName(name: String) = CITIES[name] ?: 0
    fun getCityNameById(id: Int) = CITIES.entries.find { it.value == id }?.key ?: "Unknown"
}
