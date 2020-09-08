package ru.iopump.kotest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.springframework.test.context.ContextConfiguration

/**
 * Test with [FreeSpec] in free style.
 * Context is configuring by [ContextConfiguration]
 * [Free-spec](https://github.com/kotest/kotest/blob/master/doc/styles.md#free-spec)
 * [Spring](https://github.com/kotest/kotest/blob/master/doc/extensions.md#Spring)
 */
@ContextConfiguration(classes = [AllureStep::class])
open class ReportingFreeSpec(step: AllureStep) : FreeSpec() {

    init {
        beforeTest {

        }
        "Container test" - {
            "Given the system prepared" {
                step.deployStand("http://allure.iopump.ru")
                step.applyConfig("http://allure.iopump.ru")
            }

            "When send the request" {
                step.sendRequest("http://allure.iopump.ru")
            }

            "Then positive response" {
                step.awaitRequest("http://allure.iopump.ru") shouldBe 200
            }
        }
    }
}