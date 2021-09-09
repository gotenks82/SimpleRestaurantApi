package com.example.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import kotlin.random.Random

@Schema(name="OrderItem", description="Order Item")
data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int = 1,
    val cookingTimeInMin: Long = Random.nextLong(minCookingTime, maxCookingTime),
    val orderedAt: LocalDateTime = LocalDateTime.now()
)

const val minCookingTime = 5L
const val maxCookingTime = 15L