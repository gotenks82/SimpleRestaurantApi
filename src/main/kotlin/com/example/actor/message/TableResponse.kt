package com.example.actor.message

import java.util.*

data class TableResponse<T>(val response: T? = null, val error: String? = null) {
    fun <V> fold(ifSuccess: (T) -> V, ifError: (String) -> V): Optional<V> {
        return Optional.ofNullable(
            if (response != null) {
                ifSuccess(response)
            } else if (error != null) {
                ifError(error)
            } else {
                null
            }
        )
    }
}