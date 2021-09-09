package com.example.controller.model

class ErrorResponse(val errors: List<String>) {
    companion object {
        fun from(error: String): ErrorResponse {
            return ErrorResponse(listOf(error))
        }
    }
}