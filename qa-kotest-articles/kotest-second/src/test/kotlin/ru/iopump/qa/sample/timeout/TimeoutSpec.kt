package ru.iopump.qa.sample.timeout

import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
class TimeoutSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(TimeoutSpec::class.java)
    }

    /*1*/override fun timeout(): Long = 2000 // Spec Level timeout for each TestCase not total
    /*2*/override fun invocationTimeout(): Long = 2000 // Spec Level invocation timeout for each TestCase invocation not total

    init {
        /*3*/invocationTimeout = 1500 // Spec Level invocation timeout for each TestCase invocation not total
        /*4*/timeout = 1500 // Spec Level timeout for each TestCase not total
        "should be invoked 2 times and will be successful at all".config(
            /*5*/invocations = 2,
            /*6*/invocationTimeout = 525.milliseconds,
            /*7*/timeout = 1050.milliseconds
        ) {
            log.info("test 1")
            delay(500)
        }

        "should be invoked 2 times and every will fail by invocationTimeout".config(
            invocations = 2,
            invocationTimeout = 400.milliseconds,
            timeout = 1050.milliseconds
        ) {
            log.info("test 2")
            delay(500)
        }

        "should be invoked 2 times and last will fail by total timeout".config(
            invocations = 2,
            invocationTimeout = 525.milliseconds,
            timeout = 1000.milliseconds
        ) {
            log.info("test 3")
            delay(500)
        }
    }
}