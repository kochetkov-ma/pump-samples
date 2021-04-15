package ru.iopump.qa.sample.spec

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.asClue
import io.kotest.core.config.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBePositive
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.positiveInts
import io.kotest.property.arbitrary.stringPattern
import kotlinx.coroutines.delay
import retrofit2.Response
import retrofit2.awaitResponse
import ru.iopump.qa.sample.StaticProjectConfiguration
import ru.iopump.qa.sample.StaticProjectConfiguration.objectMapper
import ru.iopump.qa.sample.StaticProjectConfiguration.wiremockClient
import ru.iopump.qa.sample.api.EmployeeService
import ru.iopump.qa.sample.model.Employee

@ExperimentalKotest
class EmployeeSpec : FreeSpec() {

    override fun concurrency(): Int = 2

    private val employeeService: EmployeeService = StaticProjectConfiguration.employeeService

    init {
        "Feature: Getting employee by id" - {

            var expectedId = 0
            "Given test environment is up and test data prepared" {
                delay(5000)
                expectedId = Arb.positiveInts().next()
            }

            lateinit var response: Response<Employee>
            "When client sent request to get the employee by id=$expectedId" {
                response = employeeService.get(expectedId).awaitResponse()
            }

            "Then server received request with id=$expectedId" {
                wiremockClient.verifyThat(
                    1,
                    WireMock.getRequestedFor(WireMock.urlPathEqualTo("/employee/$expectedId"))
                )
            }

            "And client received response with status 200 and id=$expectedId" {
                response.asClue {
                    it.code() shouldBe 200
                    it.body().shouldNotBeNull().asClue { employee ->
                        employee.name shouldBe "Max"
                        employee.id shouldBe expectedId
                    }
                }
            }
        }

        "Feature: Creating new employee" - {

            lateinit var employee: Employee
            "Given test environment is up and test data prepared" {
                delay(5000)
                employee = Employee(
                    name = Arb.stringPattern("[A-Z]{1}[a-z]{5,10}").next()
                )
            }

            lateinit var response: Response<Employee>
            "When client sent request to create new $employee" {
                response = employeeService.create(employee).awaitResponse()
            }

            val expectedJson: String = objectMapper.writeValueAsString(employee)
            "Then server received request with $employee" {
                wiremockClient.verifyThat(
                    1,
                    WireMock.postRequestedFor(WireMock.urlPathEqualTo("/employee"))
                        .withRequestBody(WireMock.equalToJson(expectedJson))
                )
            }

            "And client received response with status 200 and $employee" {
                response.asClue {
                    it.code() shouldBe 200
                    it.body().shouldNotBeNull().asClue { e ->
                        e.name shouldBe employee.name
                        e.id.shouldNotBeNull().shouldBePositive()
                    }
                }
            }
        }
    }
}