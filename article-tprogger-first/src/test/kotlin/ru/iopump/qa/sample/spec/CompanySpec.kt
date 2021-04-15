package ru.iopump.qa.sample.spec

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import io.kotest.assertions.asClue
import io.kotest.core.config.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldNotBeZero
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.positiveInts
import kotlinx.coroutines.delay
import okhttp3.Credentials
import retrofit2.Response
import retrofit2.awaitResponse
import retrofit2.create
import ru.iopump.qa.sample.StaticProjectConfiguration
import ru.iopump.qa.sample.StaticProjectConfiguration.companyService
import ru.iopump.qa.sample.StaticProjectConfiguration.containerUrl
import ru.iopump.qa.sample.StaticProjectConfiguration.wiremockContainer
import ru.iopump.qa.sample.api.CompanyService
import ru.iopump.qa.sample.http.BasicAuthInterceptor
import ru.iopump.qa.sample.http.HttpUtil
import ru.iopump.qa.sample.http.HttpUtil.jsonConverter
import ru.iopump.qa.sample.http.HttpUtil.loggingInterceptors
import ru.iopump.qa.sample.model.Company
import ru.iopump.qa.sample.model.Employee

@ExperimentalKotest
class CompanySpec : FreeSpec() {

    override fun concurrency(): Int = 2

    private val companyServiceWithAuth: CompanyService = HttpUtil.createRetrofit(
        wiremockContainer.containerUrl,
        jsonConverter,
        loggingInterceptors.plus(BasicAuthInterceptor.create("owner", "owner"))
    ).create()

    init {
        "Feature: Get company by id" - {

            var expectedId = 0
            "Given test environment is up and test data prepared" {
                delay(5000)
                expectedId = Arb.positiveInts().next()
            }

            lateinit var response: Response<Company>
            val basic = Credentials.basic("user", "user")
            "When client sent request to get the company by id=$expectedId" {
                response = companyService.get(basic, expectedId).awaitResponse()
            }

            "Then server received request with id=$expectedId and Authorization header" {
                StaticProjectConfiguration.wiremockClient.verifyThat(
                    1,
                    WireMock.getRequestedFor(WireMock.urlPathEqualTo("/company/$expectedId"))
                        .withHeader("Authorization", WireMock.equalTo(basic))
                )
            }

            "And client received response with status 200 and id=$expectedId" {
                response.asClue {
                    it.code() shouldBe 200
                    it.body().shouldNotBeNull().asClue { employee ->
                        employee.name shouldBe "MirPlatform Ltd."
                        employee.id shouldBe expectedId
                    }
                }
            }
        }

        "Feature: Get all employees in company" - {

            var expectedId = 0
            "Given test environment is up and test data prepared" {
                delay(5000)
                expectedId = Arb.positiveInts().next()
            }

            lateinit var response: Response<Collection<Employee>>
            "When client sent request to get the company by id=$expectedId" {
                response = companyServiceWithAuth.getEmployeeCollection(expectedId).awaitResponse()
            }

            "Then server received request with id=$expectedId and Authorization header" {
                StaticProjectConfiguration.wiremockClient.verifyThat(
                    1,
                    WireMock.getRequestedFor(WireMock.urlPathEqualTo("/company/$expectedId/employee"))
                        .withHeader("Authorization", AnythingPattern())
                )
            }

            "And client received response with status 200 and collection of employee" {
                response.asClue {
                    it.code() shouldBe 200
                    it.body().shouldNotBeNull().asClue { employees ->
                        employees.shouldHaveSize(2).forAll { e ->
                            e.id.shouldNotBeNull().shouldNotBeZero()
                            e.name.shouldNotBeBlank()
                        }
                    }
                }
            }
        }
    }
}