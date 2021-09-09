package com.example.controller

import com.example.actor.message.TableResponse
import com.example.controller.model.ErrorResponse
import com.example.controller.model.NewOrderItem
import com.example.controller.model.SuccessResponse
import com.example.service.TableService
import io.micronaut.http.HttpResponse.*
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.*
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.inject.Inject
import java.util.*
import javax.validation.Valid

@Controller("/table")
open class TableController @Inject constructor(
    private val tableService: TableService
) {

    @Get("/{tableId}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun getAllItems(
        tableId: Long
    ): MutableHttpResponse<out Any?> {
        return tableService.getAllItems(tableId)
            .toHttpResponse({ ok(it) }, HttpStatus.NOT_FOUND)
    }

    @Post("/{tableId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(
        ApiResponse(responseCode = "201", content = [Content(schema = Schema(name = "OrderItem"))])
    )
    open suspend fun addItem(
        tableId: Long,
        @Valid @Body item: NewOrderItem
    ): MutableHttpResponse<out Any?> {
        return tableService.addItem(tableId, item)
            .toHttpResponse({ created(it) }, HttpStatus.CONFLICT)
    }

    @Delete("/{tableId}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun clearTable(
        tableId: Long
    ): MutableHttpResponse<out Any?> {
        return tableService.clearTable(tableId)
            .toHttpResponse({ ok(SuccessResponse(it)) }, HttpStatus.CONFLICT)
    }

    @Get("/{tableId}/item/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", ref = "OrderItem")
    suspend fun getItem(
        tableId: Long,
        itemId: String
    ): MutableHttpResponse<out Any?> {
        return tableService.getItem(tableId, itemId)
            .toHttpResponse({ ok(it) }, HttpStatus.NOT_FOUND)
    }

    @Delete("/{tableId}/item/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun deleteItem(
        tableId: Long,
        itemId: String
    ): MutableHttpResponse<out Any?> {
        return tableService.deleteItem(tableId, itemId)
            .toHttpResponse({ ok(SuccessResponse(it)) }, HttpStatus.CONFLICT)
    }

    private fun <T,V> Optional<TableResponse<T>>.toHttpResponse(
        successFn: (T) -> MutableHttpResponse<V>,
        errorStatusCode: HttpStatus
    ): MutableHttpResponse<out Any?> = this.flatMap {
        it.fold(
            { success -> successFn(success) },
            { error -> errorResponse(errorStatusCode, error) }
        )
    }.orElse(notFound())

    private fun errorResponse(
        status: HttpStatus,
        error: String
    ): MutableHttpResponse<ErrorResponse> {
        return status<ErrorResponse>(status).body(ErrorResponse.from(error))
    }
}