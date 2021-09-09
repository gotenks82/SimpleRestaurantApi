package com.example.actor

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestKit
import com.example.actor.message.*
import com.example.controller.model.NewOrderItem
import com.example.model.OrderItem
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class TableActorTest {

    var system: ActorSystem? = null
    val expectDuration = Duration.create(1, TimeUnit.SECONDS)
    val tableId = 1L

    @BeforeEach
    fun setup() {
        system = ActorSystem.create()
    }

    @AfterEach
    fun teardown() {
        TestKit.shutdownActorSystem(system, Duration.create(10, TimeUnit.SECONDS), true)
        system = null
    }

    /**
     * Helper method that setup a test actor and a test sender
     */
    fun testWithTableActor(
        tableItems: MutableMap<String, OrderItem> = mutableMapOf(),
        testFn: TestKit.(ActorRef) -> Any?
    ) {
        object : TestKit(system) {
            init {
                val props = Props.create(TableActor::class.java, tableId, tableItems)
                val subject = system!!.actorOf(props, tableId.toString())
                this.testFn(subject)
            }
        }
    }

    @Nested
    inner class Get {

        @DisplayName("Get items for empty table")
        @Test
        fun getEmptyItemsTest() {
            testWithTableActor { tableActor ->
                val expectedResponse = TableResponse<List<OrderItem>>(emptyList())
                tableActor.tell(GetAllItems(tableId), this.testActor())
                expectMsg(expectDuration, expectedResponse)
            }
        }

        @DisplayName("Get items for table with orders")
        @Test
        fun getFullItemsTest() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(GetAllItems(tableId), this.testActor())

                val response = receiveOne(expectDuration) as? TableResponse<List<OrderItem>>

                assertIterableEquals(tableItems.values, response?.response)
            }
        }

        @DisplayName("Get existing Item")
        @Test
        fun getExistingItem() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(GetItem(tableId, "1"), this.testActor())

                val response = receiveOne(expectDuration) as TableResponse<OrderItem>

                assertEquals(tableItems["1"], response.response)
            }
        }

        @DisplayName("Get missing Item")
        @Test
        fun getMissingItem() {
            object : TestKit(system) {
                init {
                    val tableItems = mutableMapOf(
                        "1" to OrderItem("1", "Pizza", 1),
                        "2" to OrderItem("2", "Pasta", 1)
                    )
                    val props = Props.create(TableActor::class.java, tableId, tableItems)
                    val subject = system!!.actorOf(props, tableId.toString())

                    subject.tell(GetItem(tableId, "3"), this.testActor())

                    expectMsg(
                        expectDuration,
                        TableResponse<OrderItem>(error = "OrderItem with ID 3 not found")
                    )
                }
            }
        }
    }

    @Nested
    inner class Add {
        @DisplayName("Add Items to table")
        @Test
        fun addItemsToTable() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(
                    AddItem(tableId, NewOrderItem("3", "Pizza")), this.testActor()
                )
                val response = receiveOne(expectDuration) as? TableResponse<OrderItem>
                val createdItem = response?.response

                assertEquals("3", createdItem?.id)
                assertEquals("Pizza", createdItem?.name)
                assertEquals(1, createdItem?.quantity)
                assertNotNull(createdItem?.cookingTimeInMin)
                assertNotNull(createdItem?.orderedAt)
                assertTrue((5..15).contains(requireNotNull(createdItem).cookingTimeInMin))
                assertEquals(3, tableItems.size)
                assertTrue(tableItems.contains("3"))
            }
        }

        @DisplayName("Add same item with existing Id is idempotent")
        @Test
        fun addItemWithExistingId() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(
                    AddItem(tableId, NewOrderItem("1", "Pizza")), this.testActor()
                )
                val response = receiveOne(expectDuration) as? TableResponse<OrderItem>
                val createdItem = response?.response

                assertEquals("1", createdItem?.id)
                assertEquals("Pizza", createdItem?.name)
                assertEquals(1, createdItem?.quantity)
                assertNotNull(createdItem?.cookingTimeInMin)
                assertNotNull(createdItem?.orderedAt)
                assertTrue((5..15).contains(requireNotNull(createdItem).cookingTimeInMin))
                assertEquals(2, tableItems.size)
            }
        }

        @DisplayName("Add different item with existing Id returns error")
        @Test
        fun addDiffItemWithExistingId() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(
                    AddItem(tableId, NewOrderItem("1", "Ramen")), this.testActor()
                )
                val response = receiveOne(expectDuration) as? TableResponse<OrderItem>
                assertNull(response?.response)
                assertEquals("ID matches a different OrderItem", response?.error)
                assertEquals(2, tableItems.size)
            }
        }
    }

    @Nested
    inner class delete {

        @DisplayName("delete from empty table")
        @Test
        fun deleteFromEmptyTable() {
            testWithTableActor { tableActor ->
                tableActor.tell(DeleteItem(tableId, "1"), this.testActor())

                val response = receiveOne(expectDuration) as? TableResponse<String>

                assertEquals("Item 1 not found", response?.response)
            }
        }

        @DisplayName("delete existing item")
        @Test
        fun deleteExistingItem() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(DeleteItem(tableId, "1"), this.testActor())

                val response = receiveOne(expectDuration) as? TableResponse<String>

                assertEquals("Item 1 deleted successfully", response?.response)
                assertFalse(tableItems.contains("1"))
            }
        }

        @DisplayName("clear empty table")
        @Test
        fun clearEmptyTable() {
            val tableItems = mutableMapOf<String, OrderItem>()
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(ClearTable(tableId), this.testActor())

                val response = receiveOne(expectDuration) as? TableResponse<String>

                assertEquals("Table cleared", response?.response)
                assertTrue(tableItems.isEmpty())
            }
        }

        @DisplayName("clear non-empty table")
        @Test
        fun clearTable() {
            val tableItems = mutableMapOf(
                "1" to OrderItem("1", "Pizza", 1),
                "2" to OrderItem("2", "Pasta", 1)
            )
            testWithTableActor(tableItems) { tableActor ->
                tableActor.tell(ClearTable(tableId), this.testActor())

                val response = receiveOne(expectDuration) as? TableResponse<String>

                assertEquals("Table cleared", response?.response)
                assertTrue(tableItems.isEmpty())
            }
        }
    }
}