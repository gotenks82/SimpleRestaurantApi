package com.example.service

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestProbe
import com.example.actor.message.*
import com.example.controller.model.NewOrderItem
import com.example.model.OrderItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class TableServiceTest {
    var system: ActorSystem? = null
    val expectDuration = Duration.create(1, TimeUnit.SECONDS)
    val tableId = 1L

    lateinit var testProbe: TestProbe
    lateinit var service: TableService

    @BeforeEach
    fun setup() {
        system = ActorSystem.create()
        testProbe = object : TestProbe(system) {}
        service = TableService(java.time.Duration.ofSeconds(1), testProbe.ref())
    }

    @AfterEach
    fun teardown() {
        TestKit.shutdownActorSystem(system, Duration.create(10, TimeUnit.SECONDS), true)
        system = null
    }

    @DisplayName("GetAllItems sends the right message and returns the response")
    @Test
    fun testGetAllItems() {
        val responseFuture = GlobalScope.future {
            service.getAllItems(tableId)
        }
        val message = testProbe.receiveOne(expectDuration)
        assertTrue(message is GetAllItems)

        val getAllItems = message as GetAllItems
        assertEquals(tableId, getAllItems.tableId)

        testProbe.reply(TableResponse(listOf(OrderItem("id", "name"))))

        val response = responseFuture.get(1, TimeUnit.SECONDS)
        assertTrue(response.isPresent)

        val orderItems = response.get().response!!
        assertEquals(1, orderItems.size)
        assertEquals("id", orderItems[0].id)
        assertEquals("name", orderItems[0].name)
    }

    @DisplayName("AddItem sends the right message and returns the response")
    @Test
    fun testAddItem() {
        val responseFuture = GlobalScope.future {
            service.addItem(tableId, NewOrderItem("id","name"))
        }

        val message = testProbe.receiveOne(expectDuration)
        assertTrue(message is AddItem)

        val addItem = message as AddItem
        assertEquals("id", addItem.item.id)

        testProbe.reply(TableResponse(OrderItem(addItem.item.id, addItem.item.name)))

        val response = responseFuture.get(1, TimeUnit.SECONDS)
        assertTrue(response.isPresent)

        val orderItem = response.get().response!!
        assertEquals("id", orderItem.id)
        assertEquals("name", orderItem.name)
    }

    @DisplayName("GetItem sends the right message and returns the response")
    @Test
    fun testGetItem() {
        val responseFuture = GlobalScope.future {
            service.getItem(tableId, "id")
        }

        val message = testProbe.receiveOne(expectDuration)
        assertTrue(message is GetItem)

        val getItem = message as GetItem
        assertEquals("id", getItem.itemId)

        testProbe.reply(TableResponse(OrderItem(getItem.itemId, getItem.itemId)))

        val response = responseFuture.get(1, TimeUnit.SECONDS)
        assertTrue(response.isPresent)

        val orderItem = response.get().response!!
        assertEquals("id", orderItem.id)
        assertEquals("id", orderItem.name)
    }

    @DisplayName("DeleteItem sends the right message and returns the response")
    @Test
    fun testDeleteItem() {
        val responseFuture = GlobalScope.future {
            service.deleteItem(tableId, "id")
        }

        val message = testProbe.receiveOne(expectDuration)
        assertTrue(message is DeleteItem)

        val deleteItem = message as DeleteItem
        assertEquals("id", deleteItem.itemId)

        testProbe.reply(TableResponse("messageReceived"))

        val response = responseFuture.get(1, TimeUnit.SECONDS)
        assertTrue(response.isPresent)

        val messageString = response.get().response!!
        assertEquals("messageReceived", messageString)
    }

    @DisplayName("ClearTable sends the right message and returns the response")
    @Test
    fun testClearTable() {
        val responseFuture = GlobalScope.future {
            service.clearTable(tableId)
        }

        val message = testProbe.receiveOne(expectDuration)
        assertTrue(message is ClearTable)

        val clearTable = message as ClearTable
        assertEquals(tableId, clearTable.tableId)

        testProbe.reply(TableResponse("messageReceived"))

        val response = responseFuture.get(1, TimeUnit.SECONDS)
        assertTrue(response.isPresent)

        val messageString = response.get().response!!
        assertEquals("messageReceived", messageString)
    }
}