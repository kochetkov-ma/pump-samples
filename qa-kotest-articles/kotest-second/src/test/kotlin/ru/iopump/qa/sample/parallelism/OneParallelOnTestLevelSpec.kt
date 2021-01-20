package ru.iopump.qa.sample.parallelism

import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class OneParallelOnTestLevelSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(OneParallelOnTestLevelSpec::class.java)
    }

    override fun threads(): Int = 2

    init {
        "parallel on test level 1" {
            log.info("test 1 started")
            delay(500)
            log.info("test 1 finished")
        }

        "parallel on test level 2" {
            log.info("test 2 started")
            delay(1000)
            log.info("test 2 finished")
        }
    }
}