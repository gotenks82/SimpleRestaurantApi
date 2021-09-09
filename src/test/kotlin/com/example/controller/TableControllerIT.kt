package com.example.controller

import akka.actor.ActorRef
import com.example.actor.message.AddItem
import com.example.actor.message.ClearTable
import com.example.controller.model.NewOrderItem
import com.example.model.OrderItem
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle

@MicronautTest
class TableControllerIT {

    val tableId = 1L

    val items = listOf(
        NewOrderItem("id1", "name1", 1),
        NewOrderItem("id2", "name2", 2)
    )

    @Inject
    lateinit var server: EmbeddedServer
    lateinit var client: HttpClient

    @Inject
    lateinit var tableManagerActor: ActorRef

    @BeforeEach
    fun setup() {
        client = HttpClient.create(server.url)

        tableManagerActor.tell(ClearTable(tableId), ActorRef.noSender())
        items.forEach {
            tableManagerActor.tell(AddItem(tableId, it), ActorRef.noSender())
        }
    }

    @Nested
    @TestInstance(Lifecycle.PER_METHOD)
    inner class Get {
        @DisplayName("Get all items")
        @Test
        fun getAllItems() {
            val response = client.toBlocking().exchange(
                HttpRequest.GET<OrderItem>("/table/$tableId"),
                List::class.java
            )
            val responseItems = response.body() as List<Map<String, Any>>
            assertEquals(200, response.code())
            assertEquals(2, responseItems.size)
            assertEquals("id1", responseItems[0]["id"])
            assertEquals("id2", responseItems[1]["id"])
        }

        @DisplayName("Get Table not found")
        @Test
        fun getTableNotFound() {
            try {
                client.toBlocking().exchange(
                    HttpRequest.GET<OrderItem>("/table/$tableId"),
                    List::class.java
                )
            } catch (response: HttpClientResponseException) {
                assertEquals(HttpStatus.NOT_FOUND, response.status)
            }
        }

        @DisplayName("Get one item")
        @Test
        fun getOne() {
            val response = client.toBlocking().exchange(
                HttpRequest.GET<OrderItem>("/table/$tableId/item/id2"),
                Map::class.java
            )
            val responseItem = response.body() as Map<String, Any>
            assertEquals(200, response.code())
            assertEquals("id2", responseItem["id"])
            assertEquals("name2", responseItem["name"])
        }

        @DisplayName("Get Item not found")
        @Test
        fun getItemNotFound() {
            try {
                client.toBlocking().exchange(
                    HttpRequest.GET<OrderItem>("/table/$tableId/item/id3"),
                    Map::class.java
                )
            } catch (response: HttpClientResponseException) {
                assertEquals(HttpStatus.NOT_FOUND, response.status)
            }
        }
    }

    @Nested
    @TestInstance(Lifecycle.PER_METHOD)
    inner class Post {
        @DisplayName("Post a new item")
        @Test
        fun addOneItem() {
            val response = client.toBlocking()
                .exchange(
                    HttpRequest.POST("/table/$tableId", NewOrderItem("id3", "name3")),
                    Map::class.java
                )

            assertEquals(HttpStatus.CREATED, response.status)
            val createdItem = response.body()

            assertEquals("id3", createdItem["id"])
            assertEquals("name3", createdItem["name"])
            assertEquals(1, createdItem["quantity"])
            assertNotNull(createdItem["cookingTimeInMin"])
            assertNotNull(createdItem["orderedAt"])
        }

        @DisplayName("Post the same item twice")
        @Test
        fun addSameItemTwice() {
            val response = client.toBlocking()
                .exchange(
                    HttpRequest.POST("/table/$tableId", NewOrderItem("id1", "name1")),
                    Map::class.java
                )

            assertEquals(HttpStatus.CREATED, response.status)
            val createdItem = response.body()

            assertEquals("id1", createdItem["id"])
            assertEquals("name1", createdItem["name"])
            assertEquals(1, createdItem["quantity"])
            assertNotNull(createdItem["cookingTimeInMin"])
            assertNotNull(createdItem["orderedAt"])
        }

        @DisplayName("Post with ID Conflict returns error")
        @Test
        fun addDifferentItemSameId() {
            try {
                client.toBlocking()
                    .exchange(
                        HttpRequest.POST("/table/$tableId", NewOrderItem("id1", "name3")),
                        Map::class.java
                    )
            } catch (response: HttpClientResponseException) {
                assertEquals(HttpStatus.CONFLICT, response.status)
            }
        }
    }

    @Nested
    @TestInstance(Lifecycle.PER_METHOD)
    inner class Delete {
        @DisplayName("Delete one item")
        @Test
        fun deleteOne() {
            val response = client.toBlocking()
                .exchange(
                    HttpRequest.DELETE("/table/$tableId/item/id1", null),
                    Map::class.java
                )

            assertEquals(HttpStatus.OK, response.status)
            assertEquals("Item id1 deleted successfully", response.body()["message"])
        }

        @DisplayName("Delete all items")
        @Test
        fun deleteAll() {
            val response = client.toBlocking().exchange(
                HttpRequest.DELETE("/table/$tableId", null),
                Map::class.java
            )

            assertEquals(HttpStatus.OK, response.status)
            assertEquals("Table cleared", response.body()["message"])
        }

        @DisplayName("Delete missing item still returns 200 for idempotency")
        @Test
        fun deleteMissing() {
            val response = client.toBlocking()
                .exchange(
                    HttpRequest.DELETE("/table/$tableId/item/id3", null),
                    Map::class.java
                )
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("Item id3 not found", response.body()["message"])
        }

        @DisplayName("Delete item from missing table returns success")
        @Test
        fun deleteFromMissingTable() {
            val response = client.toBlocking().exchange(
                HttpRequest.DELETE("/table/2/item/itemId", null),
                Map::class.java
            )

            assertEquals(HttpStatus.OK, response.status)
            assertEquals("Item itemId not found", response.body()["message"])
        }

        @DisplayName("Delete all items from mising table returns success")
        @Test
        fun deleteAllFromMissingTable() {
            val response = client.toBlocking().exchange(
                HttpRequest.DELETE("/table/2", null),
                Map::class.java
            )
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("Table cleared", response.body()["message"])
        }
    }
}