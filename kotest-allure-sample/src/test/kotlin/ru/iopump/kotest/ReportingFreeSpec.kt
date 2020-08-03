package ru.iopump.kotest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [AllureStep::class])
open class ReportingFreeSpec(step: AllureStep) : FreeSpec() {

    init {
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