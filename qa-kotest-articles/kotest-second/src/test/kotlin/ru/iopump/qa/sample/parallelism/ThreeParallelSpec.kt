package ru.iopump.qa.sample.parallelism

import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class ThreeParallelSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(ThreeParallelSpec::class.java)
    }

    init {
        "sequential test 1" {
            log.info("test 1 started")
            delay(1000)
            log.info("test 1 finished")
        }

        "sequential test 2" {
            log.info("test 2 started")
            delay(1000)
            log.info("test 2 finished")
        }
    }
}