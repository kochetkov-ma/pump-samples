package ru.iopump

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.*
import kotlin.concurrent.thread

open class DockerComposeTest : StringSpec() {

    init {
        "launch - print only 2" {

            val job = GlobalScope.launch {
                (1..5).forEach { index ->
                    delay(100)
                    println("Index = $index")
                }
            }

            delay(200)
        }

        "thread - create 50 threads without waiting" {
            (1..10).forEach { index ->
                thread(start = true) {
                    println("[$index] Thread = " + Thread.currentThread())
                }
            }
        }

        "coroutines - create 50 coroutines without waiting" {
            (1..50).forEach { index ->
                GlobalScope.launch {
                    println("[$index] Thread = " + Thread.currentThread())
                }
            }
        }

        "deferred coroutines - create 500 and await" {
            val deferred = (1..500).map { index ->
                GlobalScope.async {
                    delay(500)
                    println("[$index] Thread = " + Thread.currentThread())
                    index
                }
            }
            val sum = deferred.sumOf { it.await() }
            println("Sum $sum")
        }

        "coroutines with scope and repeat" {
            runBlocking { // this: CoroutineScope
                repeat(10) { index ->
                    launch {
                        delay(400L)
                        println("[$index] Task from runBlocking")
                    }
                }

                coroutineScope { // Creates a coroutine scope
                    launch {
                        delay(500L)
                        println("Task from nested launch")
                    }

                    delay(100L)
                    println("Task from coroutine scope") // This line will be printed before the nested launch
                }

                println("Coroutine scope is over") // This line is not printed until the nested launch completes
            }
        }

        "cancellation blocking" {
            val job = launch {
                try {
                    repeat(5) { i ->
                        // No 'suspend' function in this block
                        if (isActive) {
                            println("job: I'm sleeping $i ...")

                            try {
                                runInterruptible(Dispatchers.IO) {
                                    try {
                                        Thread.sleep(500) // This method cannot be cancel as coroutine, but runInterruptible - works
                                    } catch (e: InterruptedException) {
                                        println("Try cancel blocking code by $e")
                                        println("But i want use another way")
                                    }
                                }
                            } catch (e: CancellationException) {
                                println("Try cancel coroutine by $e")
                                println("But i want use another way")
                            }

                        } else {
                            println("\nGREAT! Cancelled by user\n")
                            throw CancellationException("Cancelled by user")
                        }
                    }
                } catch (e: Throwable) {
                    println("Cancel by $e")
                } finally {
                    withContext(NonCancellable) {
                        println("job: I'm running finally")
                        delay(1000L)
                        println("job: And I've just delayed for 1 sec because I'm non-cancellable")
                    }
                }
            }
            delay(250L) // delay a bit
            println("main: I'm tired of waiting!")
            job.cancelAndJoin()
            println("main: Now I can quit.")
        }
    }
}