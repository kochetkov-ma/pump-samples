package org.brewcode.kotest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.extensions.spring.testContextManager
import io.kotest.matchers.shouldBe
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Story
import io.restassured.RestAssured
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.PropertyResolver
import org.springframework.http.MediaType
import ru.iopump.kotest.allure.annotation.KDescription
import ru.iopump.kotest.allure.annotation.KJira

@Epic("Habr")
@Feature("Kotest")
@Story("Validation")
@Link(name = "Requirements", url = "https://habr.com/ru/company/nspk/blog/")
@KJira("KT-1")
@KDescription(
    """
Kotest integration with Spring Boot.
Also using Allure Listener for test reporting.
    """
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ValidationControllerKotestTest(
    @Value("\${local.server.port}") private val localServerPort: String,
    propertyResolverInConstructor: PropertyResolver
) : FreeSpec() {

    private lateinit var body: RequestSpecification
    private lateinit var post: Response

    init {
        table(
            headers("Text field", "Expected Code", "Expected Message"),
            row("Hello", 0, "Ok"),
            row(null, 1, "Null text"),
            row("    ", 2, "Blank text")
        ).forAll { text, expectedCode, expectedMessage ->

            "Scenario: Validation for text '$text'" - {

                "Given spring context injected successfully" {
                    val appContextExample = testContextManager().testContext.applicationContext

                    appContextExample.environment.getProperty("local.server.port") shouldBe localServerPort
                    propertyResolverInConstructor.getProperty("local.server.port") shouldBe localServerPort
                }

                "Given POST request prepared with text '$text'" {
                    body = RestAssured
                        .with()
                        .log()
                        .all()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body(RequestDto(text))
                }

                "When request sent" {
                    post = body.post("http://localhost:$localServerPort/validation")
                }

                "Then response with body code $expectedCode and body message '$expectedMessage'" {
                    post
                        .then()
                        .log()
                        .all()
                        .statusCode(200)
                        .body("code", Matchers.equalTo(expectedCode))
                        .body("message", Matchers.equalTo(expectedMessage))
                }
            }
        }
    }
}