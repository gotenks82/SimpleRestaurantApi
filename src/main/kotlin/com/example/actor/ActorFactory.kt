package com.example.actor

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import io.micronaut.context.annotation.Factory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Duration

@Factory
class ActorFactory {

    @Singleton
    fun actorSystem(): ActorSystem = ActorSystem.create("Restaurant")

    @Singleton
    @Inject
    fun tableManagerActor(actorSystem: ActorSystem): ActorRef =
        actorSystem.actorOf(Props.create(TableManagerActor::class.java))

    @Singleton
    fun askTimeout(): Duration = Duration.ofSeconds(1)
}