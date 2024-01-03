package com.example.kaafkaproject
import java.io.Serializable
import java.text.NumberFormat
import java.util.*



data class MenuItem(
    val name: String,
    val price: Int,
    val imageResId: Int,
    var quantity: Int = 0
) : Serializable {
}
