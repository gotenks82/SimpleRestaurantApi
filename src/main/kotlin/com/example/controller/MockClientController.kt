package com.example.controller

import com.example.controller.model.NewOrderItem
import com.example.controller.model.SuccessResponse
import com.example.model.OrderItem
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse.ok
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

@Controller("/mock/client")
class MockClientController @Inject constructor(
    @Client(value = "/", path = "/table")
    private val client: HttpClient
) {
    private val logger: Logger = LoggerFactory.getLogger(MockClientController::class.java)

    private val clientJobs = mutableListOf<Job>()

    private val exec: ExecutorService = Executors.newFixedThreadPool(10)

    @Get("/start/{numClient}")
    fun startMockClient(
        numClient: Int
    ): MutableHttpResponse<SuccessResponse> {
        startClients(numClient)
        return ok(SuccessResponse("Started $numClient mock clients"))
    }

    @Get("/stop")
    fun stopMockClient(): MutableHttpResponse<SuccessResponse> {
        stopClients()
        return ok(SuccessResponse("Mock Clients stopped"))
    }

    private fun startClients(numClient: Int) {
        repeat(numClient) { index ->
            clientJobs.add(CoroutineScope(exec.asCoroutineDispatcher()).launch {
                while (true) {
                    try {
                        clientScenario(index)
                    } catch (_: Throwable) { }
                    delay(Random.nextLong(100, 1000))
                }
            })
        }
    }

    private fun stopClients() {
        clientJobs.forEach { it.cancel() }
        clientJobs.clear()
    }

    private suspend fun clientScenario(clientIndex: Int) {
        // pick a table
        val table = Random.nextInt(0, 200).toString()
        logger.info("Client $clientIndex starting the scenario with table $table")

        // Add a few items
        val items: List<NewOrderItem> = IntRange(1, Random.nextInt(2, 5)).map {
            NewOrderItem(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString().take(8)
            )
        }

        logger.info("Client $clientIndex adding ${items.size} items to table $table")
        items.map {
            Mono.from(client.exchange(HttpRequest.POST("/$table", it), Map::class.java))
                .awaitIgnoreException()
        }.forEach {
            it.ifPresent { assert(it.code() == 201) }
        }

        logger.info("Client $clientIndex getting all items for table $table")
        Mono.from(
            client.exchange(
                HttpRequest.GET<OrderItem>("/$table"),
                List::class.java
            )
        ).awaitIgnoreException().ifPresent { assert(it.body().size == items.size) }

        items.random().let { item ->
            logger.info("Client $clientIndex getting item ${item.id} for table $table")
            Mono.from(
                client.exchange(
                    HttpRequest.GET<OrderItem>("/$table/item/${item.id}"),
                    OrderItem::class.java
                )
            ).awaitIgnoreException().ifPresent { assert(it.body()?.id == item.id) }
        }

        // remove one item
        items.random().let {
            logger.info("Client $clientIndex deleting item ${it.id} for table $table")
            Mono.from(
                client.exchange(
                    HttpRequest.DELETE("/$table/item/${it.id}", null),
                    String::class.java
                )
            )
        }.awaitIgnoreException().ifPresent { assert(it.code() == 200) }

        // clear table
        logger.info("Client $clientIndex clearing table $table")
        Mono.from(
            client.exchange(
                HttpRequest.DELETE("/$table", null),
                String::class.java
            )
        ).awaitIgnoreException().ifPresent { assert(it.code() == 200) }
    }

    private suspend fun <T> Mono<T>.awaitIgnoreException(): Optional<T> = try {
        Optional.ofNullable(this.awaitSingleOrNull())
    } catch (t: Throwable) {
        logger.warn(t.message)
        Optional.empty<T>()
    }
}