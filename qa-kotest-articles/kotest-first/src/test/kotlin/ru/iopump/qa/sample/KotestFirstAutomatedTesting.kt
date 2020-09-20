package ru.iopump.qa.sample

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.TestType
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
        ///// ALL IN INVOCATION ORDER /////
        afterProject { }
        //// BEFORE ////
        beforeSpec { spec ->
            log.info("[BEFORE][1] beforeSpec '$spec'")
        }
        beforeContainer { onlyContainerTestType ->
            log.info("[BEFORE][2][${TestType.Container}] beforeContainer onlyContainerTestType '$onlyContainerTestType'")
        }
        beforeEach { onlyTestCaseType ->
            log.info("[BEFORE][3][${TestType.Test}] beforeEach onlyTestCaseType '$onlyTestCaseType'")
        }
        beforeAny { containerOrTestCaseType ->
            log.info("[BEFORE][4][${TestType.Container}][${TestType.Test}] beforeAny containerOrTestCaseType '$containerOrTestCaseType'")
        }
        beforeTest { anyTestCaseType ->
            log.info("[BEFORE][5] beforeTest anyTestCaseType '$anyTestCaseType'")
        }

        //// AFTER ////
        afterTest { anyTestCaseTypeWithResult ->
            log.info("[AFTER][1] afterTest anyTestCaseTypeWithResult '$anyTestCaseTypeWithResult'")
        }
        afterAny { containerOrTestCaseTypeAndResult ->
            log.info("[AFTER][2][${TestType.Container}][${TestType.Test}] afterAny containerOrTestCaseTypeAndResult '$containerOrTestCaseTypeAndResult'")
        }
        afterEach { onlyTestCaseTypeAndResult ->
            log.info("[AFTER][3][${TestType.Test}] afterEach onlyTestCaseTypeAndResult '$onlyTestCaseTypeAndResult'")
        }
        afterContainer { onlyContainerTestTypeAndResult ->
            log.info("[AFTER][4][${TestType.Container}] afterContainer onlyContainerTestTypeAndResult '$onlyContainerTestTypeAndResult'")
        }
        afterSpec { specWithoutResult ->
            log.info("[AFTER][5] afterSpec specWithoutResult '$specWithoutResult'")
        }

        //// AT THE END ////
        finalizeSpec { specWithAllResults ->
            log.info("[FINALIZE][LAST] finalizeSpec specWithAllResults '$specWithAllResults'")
        }

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