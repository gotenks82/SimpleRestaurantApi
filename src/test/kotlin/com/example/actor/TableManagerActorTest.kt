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

class TableManagerActorTest {
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
    private fun testWithTableManagerActor(
        tables: MutableMap<Long, ActorRef> = mutableMapOf(),
        testFn: TestKit.(ActorRef) -> Any?
    ) {
        object : TestKit(system) {
            init {
                val props = Props.create(TableManagerActor::class.java, tables)
                val subject = system!!.actorOf(props, "Restaurant")
                this.testFn(subject)
            }
        }
    }

    @DisplayName("Creates new ActorRef for missing table")
    @Test
    fun createsActorRefForMissingTable() {
        val tableMap: MutableMap<Long, ActorRef> = mutableMapOf()
        testWithTableManagerActor(tableMap) { tableManagerActor ->
            val expectedResponse = TableResponse<List<OrderItem>>(emptyList())
            tableManagerActor.tell(GetAllItems(tableId), this.testActor())
            expectMsg(expectDuration, expectedResponse)
            assertTrue(tableMap.contains(tableId))
            assertEquals(
                "akka://default/user/Restaurant/$tableId",
                tableMap[tableId]?.path().toString()
            )
        }
    }

    @DisplayName("Uses existing ActorRef for existing table")
    @Test
    fun usesExistingActorRefForTable() {
        val tableMap: MutableMap<Long, ActorRef> = mutableMapOf()
        testWithTableManagerActor(tableMap) { tableManagerActor ->
            tableMap[tableId] = this.testActor()

            tableManagerActor.tell(GetAllItems(tableId), ActorRef.noSender())
            expectMsg(expectDuration, GetAllItems(tableId))

            assertTrue(tableMap.contains(tableId))
            assertTrue(
                requireNotNull(tableMap[tableId]).path().toString()
                    .contains("akka://default/system/testActor")
            )
        }
    }

    @DisplayName("Does not reply for unknown messages")
    @Test
    fun doesNotReplyForUnknown() {
        val tableMap: MutableMap<Long, ActorRef> = mutableMapOf()
        testWithTableManagerActor(tableMap) { tableManagerActor ->
            tableManagerActor.tell("unknown", this.testActor())
            expectNoMessage(expectDuration)
        }
    }
}