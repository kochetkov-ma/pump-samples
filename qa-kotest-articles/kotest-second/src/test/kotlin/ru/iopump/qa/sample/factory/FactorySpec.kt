package ru.iopump.qa.sample.factory

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.freeSpec
import io.kotest.core.spec.style.scopes.FreeScope
import io.kotest.core.test.TestType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FactorySpec : FreeSpec() {

    init {
        /*1.1*/include(containerFactory(1, 2, log))
        "root container" - {
            /*2.1*/containerTemplate()
        }
    }
}

/** Kotest factory */
fun containerFactory(argument1: Any, argument2: Any, logger: Logger) =
    /*1.2*/freeSpec {
    beforeContainer { logger.info("This 'beforeContainer' callback located in the test factory") }

    "factory container" - {
        "factory test with argument1 = $argument1" { }
        "factory test with argument2 = $argument2" { }
    }
}

/** Add [TestType.Container] by scoped function extension */
/*2.2*/suspend inline fun FreeScope.containerTemplate() = apply {
    "template container with FreeScope context" - {
        /*2-3*/testCaseTemplate()
    }
}

/** Add [TestType.Test] by scoped function extension */
/*2.4*/suspend inline fun FreeScope.testCaseTemplate() = apply {
    "nested template testcase with FreeScope context" { }
}

private val log = LoggerFactory.getLogger(FactorySpec::class.java)

