package com.example.actor.message

import com.example.controller.model.NewOrderItem

sealed interface TableMessage {
    val tableId: Long
}

data class AddItem(override val tableId: Long, val item: NewOrderItem) : TableMessage
data class DeleteItem(override val tableId: Long, val itemId: String) : TableMessage
data class GetItem(override val tableId: Long, val itemId: String) : TableMessage
data class GetAllItems(override val tableId: Long) : TableMessage
data class ClearTable(override val tableId: Long) : TableMessage

