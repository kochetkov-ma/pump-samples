package ru.iopump.qa.sample

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row

import io.kotest.matchers.shouldBe

import org.slf4j.LoggerFactory
import java.util.*

open class DataDrivenTesting : FreeSpec() {

    private companion object {
        private val log = LoggerFactory.getLogger(DataDrivenTesting::class.java)
    }

    init {
        "Scenario. Single case" - {

            //region Variables
            val testEnvironment = Server()
            val tester = Client()
            //endregion

            "Given server is up. Will execute only one time" {
                testEnvironment.start()
            }

            forAll(
                row(1, UUID.randomUUID().toString()),
                row(2, UUID.randomUUID().toString())
            ) { index, uuid ->
                log.info("[ITERATION][$index] with uuid=$uuid")

                "When request prepared and sent [$index]" {
                    val request = Request(uuid)
                    tester.send(request)
                }

                "Then response received [$index]" {
                    tester.receive().code shouldBe 200
                }
            }
        }
    }
}