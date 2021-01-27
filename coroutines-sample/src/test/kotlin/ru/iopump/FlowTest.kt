package ru.iopump

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(FlowTest::class.java)

@ExperimentalCoroutinesApi
open class FlowTest : StringSpec() {

    private val channelMap = ConcurrentHashMap<Int, BroadcastChannel<DeferredContainer>>()
    private val supervisorJob = SupervisorJob()
    private val userCoroutineScope = CoroutineScope(context = Dispatchers.Default + supervisorJob)


    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() = runBlocking {
                log.info("Gracefully shutting down")
                supervisorJob.children.toList().joinAll()
                supervisorJob.cancelAndJoin()
                log.info("Gracefully shut!!!")
            }
        })

        "test 1" {
            log.info("Test coroutine start")


            userCoroutineScope.launch {
                log.info("Stage 1")
                channelMap
                    .computeIfAbsent(1) { BroadcastChannel(1) }
                    .send(DeferredContainer(startGettingResourceList()))
            }

            log.info("Test coroutine end")
        }

        "test 2" {
            log.info("Next test coroutine start")

            userCoroutineScope.launch {
                log.info("Stage 2")
                channelMap.getValue(1).openSubscription().consume {
                    processResources(this.receive())
                }
            }

            userCoroutineScope.launch {
                log.info("Stage 3")
                channelMap.getValue(1).openSubscription().consume {
                    processResources(this.receive())
                }
            }

            delay(200)
            log.info("Next test coroutine end")
        }
    }
}

class DeferredContainer(private val deferredList: List<Deferred<Int>>) {

    suspend fun getAllSuspend() = deferredList.awaitAll()
}


suspend fun startGettingResourceList(): List<Deferred<Int>> =
    coroutineScope {
        log.info("Start getting all resources")
        (1..10).toList().map { index ->
            async {
                log.info("Start getting resource [$index]")
                getResource(index)
            }
        }
    }

suspend fun processResources(container: DeferredContainer) = coroutineScope {
    log.info("Call second before")
    container.getAllSuspend().asFlow().collect { log.info("Got deferred value: $it") }
    log.info("Call second after")
}

suspend fun getResource(index: Int): Int {
    log.info("Getting some resource [$index]")
    delay(1000)
    log.info("Got some resource [$index]")
    return index
}