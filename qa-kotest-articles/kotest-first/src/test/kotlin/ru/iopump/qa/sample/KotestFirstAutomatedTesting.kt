package ru.iopump.qa.sample

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeBlank
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory

open class KotestFirstAutomatedTesting : FreeSpec() {

    private companion object {
        private val log = LoggerFactory.getLogger(KotestFirstAutomatedTesting::class.java)
    }

    init {
        "Scenario. Single case" - {

            //region Variables
            val expectedCode = 200
            val testEnvironment = Server()
            val tester = Client()
            //endregion

            "Given server is up" {
                testEnvironment.start()
            }

            "When request prepared and sent" {
                val request = Request()
                tester.send(request)
            }

            lateinit var response: Response
            "Then response received" {
                response = tester.receive()
            }

            "And has $expectedCode code" {
                assertSoftly {
                    response.asClue {
                        it.code shouldBe expectedCode
                        it.body.shouldNotBeBlank()
                    }
                }

                val assertion = assertThrows<AssertionError> {
                    assertSoftly {
                        response.asClue {
                            it.code shouldBe expectedCode + 10
                            it.body.shouldBeBlank()
                        }
                    }
                }
                assertion.message shouldContain "The following 2 assertions failed"

                log.error("Expected assertion", assertion)
            }
        }
    }
}