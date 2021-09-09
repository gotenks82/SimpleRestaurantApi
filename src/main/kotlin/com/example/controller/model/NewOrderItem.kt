package com.example.controller.model

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Introspected
data class NewOrderItem(
    @field:NotBlank val id: String,
    @field:NotBlank val name: String,
    @field:Positive val quantity: Int = 1
)
