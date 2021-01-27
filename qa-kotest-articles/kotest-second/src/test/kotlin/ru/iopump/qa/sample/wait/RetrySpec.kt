package ru.iopump.qa.sample.wait

import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@ExperimentalTime
class RetrySpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(RetrySpec::class.java)
    }

    private lateinit var tries: Iterator<Boolean>
    private lateinit var counter: AtomicInteger
    private val num: Int get() = counter.incrementAndGet()

    init {
        beforeTest {
            tries = listOf(false, false, true).iterator()
            counter = AtomicInteger()
        }

        "retry should be failed after 10 tries" {
            shouldThrow<AssertionError> {
                retry(10, 1.seconds, 50.milliseconds, 2, IllegalStateException::class) {
                    log.info("Try $num")
                    throw IllegalStateException()
                }
            }.toString().also(log::error)
        }
    }
}