package ru.iopump.qa.sample.parallelism

import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class TwoParallelSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(TwoParallelSpec::class.java)
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