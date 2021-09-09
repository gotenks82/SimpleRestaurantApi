package com.example.controller.exception

import com.example.controller.model.ErrorResponse
import io.micronaut.context.annotation.Primary
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.server.exceptions.response.ErrorContext
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor
import jakarta.inject.Singleton

@Singleton
@Primary
class ValidationErrorProcessor : ErrorResponseProcessor<ErrorResponse> {

    @NonNull
    override fun processResponse(
        @NonNull errorContext: ErrorContext,
        @NonNull response: MutableHttpResponse<*>
    ): MutableHttpResponse<ErrorResponse> {
        return if (!errorContext.hasErrors()) {
            ErrorResponse.from(response.status.reason)
        } else {
            ErrorResponse(errorContext.errors.map { it.message })
        }.let { response.body(it).contentType(MediaType.APPLICATION_JSON_TYPE) }
    }
}