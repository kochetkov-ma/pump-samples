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
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Story
import okhttp3.Credentials
import retrofit2.Response
import retrofit2.awaitResponse
import ru.iopump.qa.sample.RegistryAndProjectConfiguration.employeeService
import ru.iopump.qa.sample.RegistryAndProjectConfiguration.wiremockClient
import ru.iopump.qa.sample.model.Employee

@ExperimentalKotest
@Epic("Tproger example")
@Feature("Employee endpoint")
@Story("CRUD")
@Link("tproger.ru", url = "https://tproger.ru/")
class EmployeeSpec : FreeSpec() {

    override fun concurrency(): Int = 2

    init {
        "Scenario: Getting employee by id" - {

            var expectedId = 0
            "Given test environment is up and test data prepared" {
                expectedId = Arb.positiveInts().next()
            }

            lateinit var response: Response<Employee>
            "When client sent request to get the employee by id=$expectedId" {
                response = employeeService.get(expectedId).awaitResponse()
            }

            "Then client received response with status 200 and id=$expectedId" {
                response.asClue {
                    it.code() shouldBe 200
                    it.body().shouldNotBeNull().asClue { employee ->
                        employee.name shouldBe "Max"
                        employee.id shouldBe expectedId
                    }
                }
            }
        }

        "Scenario: Creating new employee" - {

            lateinit var employee: Employee
            "Given test environment is up and test data prepared" {
                employee = Employee(
                    name = Arb.stringPattern("[A-Z]{1}[a-z]{5,10}").next()
                )
            }

            lateinit var response: Response<Employee>
            val basic = Credentials.basic("user", "user")
            "When client sent request to create new $employee" {
                response = employeeService.create(basic, employee).awaitResponse()
            }

            "Then server received request with $employee" {
                wiremockClient.verifyThat(
                    1,
                    WireMock.postRequestedFor(WireMock.urlPathEqualTo("/employee"))
                        .withHeader("Authorization", WireMock.equalTo(basic))
                )
            }

            "And client received response with status 200 with generated id" {
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