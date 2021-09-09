package com.example.actor

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import com.example.actor.message.TableMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TableManagerActor @JvmOverloads constructor(
    private val tables: MutableMap<Long, ActorRef> = mutableMapOf()
): UntypedAbstractActor() {
    private val logger: Logger = LoggerFactory.getLogger(TableManagerActor::class.java)

    override fun onReceive(message: Any?) {
        when(message) {
            is TableMessage -> getTableActor(message.tableId).forward(message, context)
            else -> logger.warn("Unknown message $message")
        }
    }

    private fun getTableActor(id: Long) : ActorRef {
        return tables.computeIfAbsent(id) {
            context.actorOf(
                Props.create(TableActor::class.java, id),
                id.toString()
            )
        }
    }
}