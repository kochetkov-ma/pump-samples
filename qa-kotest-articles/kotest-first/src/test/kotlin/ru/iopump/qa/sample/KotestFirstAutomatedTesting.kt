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
            val server = Server()
            val client = Client()
            //endregion

            "Given server is up" {
                server.start()
            }

            "When request prepared and sent" {
                val request = Request()
                client.send(request)
            }

            "Then response received and has $expectedCode code" {
                val response = client.receive()

                assertSoftly {
                    response.asClue {
                        it.code shouldBe 200
                        it.body.shouldNotBeBlank()
                    }
                }

                val assertion = assertThrows<AssertionError> {
                    assertSoftly {
                        response.asClue {
                            it.code shouldBe 100
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