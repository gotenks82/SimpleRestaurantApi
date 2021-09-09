package com.example.service

import akka.actor.ActorRef
import akka.pattern.AskTimeoutException
import akka.pattern.Patterns.ask
import com.example.actor.message.*
import com.example.controller.model.NewOrderItem
import com.example.model.OrderItem
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

@Singleton
class TableService @Inject constructor(
    private val askTimeout: Duration,
    private val tableManagerActor: ActorRef
) {
    private val logger: Logger = LoggerFactory.getLogger(TableService::class.java)

    suspend fun getAllItems(tableId: Long): Optional<TableResponse<List<OrderItem>>> {
        return askTable(GetAllItems(tableId))
    }

    suspend fun addItem(tableId: Long, item: NewOrderItem): Optional<TableResponse<OrderItem>> {
        return askTable(AddItem(tableId, item))
    }

    suspend fun getItem(tableId: Long, itemId: String): Optional<TableResponse<OrderItem>> {
        return askTable(GetItem(tableId, itemId))
    }

    suspend fun deleteItem(tableId: Long, itemId: String): Optional<TableResponse<String>> {
        return askTable(DeleteItem(tableId, itemId))
    }

    suspend fun clearTable(tableId: Long): Optional<TableResponse<String>> {
        return askTable(ClearTable(tableId))
    }

    private suspend fun <T> askTable(message: TableMessage): Optional<TableResponse<T>> {
        return try {
            ask(tableManagerActor, message, askTimeout)
                .toCompletableFuture()
                .await()
                .let { it as? TableResponse<T> }
                .let { Optional.ofNullable(it) }
        } catch (timeout: AskTimeoutException) {
            logger.warn("Timeout when asking: $message")
            Optional.of(TableResponse(error = "Your request Timed out, please try again"))
        } catch (t: Throwable) {
            logger.error("unexpected error:", t)
            Optional.of(TableResponse(error = "Unexpected error: ${t.localizedMessage}"))
        }
    }
}