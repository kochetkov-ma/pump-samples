package ru.iopump.qa.sample.wait

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.timing.continually
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FreeSpec
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
class WaitSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(WaitSpec::class.java)
    }

    /*1*/
    private lateinit var tries: Iterator<Boolean>
    private lateinit var counter: AtomicInteger
    private val num: Int get() = counter.incrementAndGet()

    init {
        /*2*/
        beforeTest {
            tries = listOf(false, false, true).iterator()
            counter = AtomicInteger()
        }

        "eventually waiting should be success" {
            /*3*/eventually(200.milliseconds, 50.milliseconds, IllegalStateException::class) {
            log.info("Try #$num")
            if (tries.next().not()) /*4*/ throw IllegalStateException("Try #$counter")
        }
        }

        "eventually waiting should be failed on second try" {
            /*5*/shouldThrow<AssertionError> {
            eventually(/*6*/100.milliseconds, 50.milliseconds, IllegalStateException::class) {
                log.info("Try #$num")
                if (tries.next().not()) throw IllegalStateException("Try #$counter")
            }
        }.toString().also(log::error)
        }

        "continually waiting should be success" - {
            /*7*/continually(200.milliseconds, 50.milliseconds) {
            log.info("Try #$num")
        }
        }

        "continually waiting should be failed on third try" {
            /*8*/shouldThrow<IllegalStateException> {
            continually(200.milliseconds, 50.milliseconds) {
                log.info("Try #$num")
                if (tries.next()) throw IllegalStateException("Try #$counter")
            }
        }.toString().also(log::error)
        }
    }
}