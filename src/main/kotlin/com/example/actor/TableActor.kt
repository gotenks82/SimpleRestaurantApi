package com.example.actor

import akka.actor.UntypedAbstractActor
import com.example.actor.message.*
import com.example.controller.model.NewOrderItem
import com.example.model.OrderItem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class TableActor @JvmOverloads constructor(
    private val tableId: Long,
    private val items: MutableMap<String, OrderItem> = mutableMapOf()
) : UntypedAbstractActor() {
    private val logger: Logger = LoggerFactory.getLogger(TableActor::class.java)

    override fun onReceive(message: Any?) {
        if (message is TableMessage && message.tableId != tableId) {
            answer(getErrorResponse("Message sent to the wrong table"))
        }
        when (message) {
            is AddItem -> answer(addItem(message.item))
            is DeleteItem -> answer(deleteItem(message.itemId))
            is GetItem -> answer(getItem(message.itemId))
            is GetAllItems -> answer(getAllItems())
            is ClearTable -> answer(clearItems())
            else -> answer(getErrorResponse("Unknown Message sent to the table $tableId"))
        }
    }

    private fun answer(response: Any?) = sender.tell(response, self)

    private fun addItem(newItem: NewOrderItem): TableResponse<OrderItem> {
        return items.computeIfAbsent(newItem.id) { newItem.toOrderItem() }
            .takeIf { it.isSameOrderAs(newItem) }
            ?.let {
                TableResponse(it).also { logger.info("Table $tableId: order received: $newItem") }
            } ?: TableResponse<OrderItem>(error = "ID matches a different OrderItem")
            .also { logger.info("Table $tableId: ID matches a different OrderItem") }
    }

    private fun getItem(itemId: String): TableResponse<OrderItem> {
        return items[itemId]?.let {
            TableResponse(it).also { logger.info("Table $tableId: Retrieve item: $itemId") }
        } ?: TableResponse<OrderItem>(
            error = "OrderItem with ID $itemId not found"
        ).also { logger.info("Table $tableId: OrderItem with ID $itemId not found") }
    }

    private fun deleteItem(itemId: String): TableResponse<String> {
        return items.remove(itemId)?.let {
            TableResponse("Item $itemId deleted successfully")
                .also { logger.info("Table $tableId: Deleted Item $itemId") }
        } ?: TableResponse("Item $itemId not found").also {
            logger.info("Table $tableId: Item $itemId not found")
        }
    }

    private fun getAllItems(): TableResponse<List<OrderItem>> =
        TableResponse(items.values.sortedBy { it.orderedAt })

    private fun clearItems(): TableResponse<String> {
        items.clear()
        return TableResponse("Table cleared")
    }

    private fun getErrorResponse(message: String): TableResponse<Nothing> {
        return TableResponse(error = message)
    }

    private fun NewOrderItem.toOrderItem() = OrderItem(
        id = this.id,
        name = this.name,
        quantity = this.quantity
    )

    private fun OrderItem.isSameOrderAs(newItem: NewOrderItem): Boolean {
        return NewOrderItem(this.id, this.name, this.quantity) == newItem
    }

}