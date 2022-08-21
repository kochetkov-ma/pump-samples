package org.brewcode.kotest

import io.restassured.RestAssured
import org.hamcrest.Matchers
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ValidationControllerJunitTest {

    @Value("\${local.server.port}")
    private val localServerPort = 0

    @ParameterizedTest(name = "Validation for {0}")
    @CsvSource(
        textBlock = """
      Text field | Expected Code | Expected Message
      Hello      | 0             | Ok
      null       | 1             | Null text
      '   '      | 2             | Blank text""", delimiter = '|', useHeadersInDisplayName = true, nullValues = ["null"]
    )
    fun testSampleGetEndpointTextNull(text: String?, expectedCode: Int, expectedMessage: String) {
        RestAssured
            .with()
            .log()
            .all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(RequestDto(text))
            .post("http://localhost:$localServerPort/validation")
            .then()
            .log()
            .all()
            .statusCode(200)
            .body("code", Matchers.equalTo(expectedCode))
            .body("message", Matchers.equalTo(expectedMessage))
    }
}