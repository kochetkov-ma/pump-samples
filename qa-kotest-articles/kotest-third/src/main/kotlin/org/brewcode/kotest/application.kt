package org.brewcode.kotest

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    SpringApplication.run(SampleApplication::class.java, *args)
}

@SpringBootApplication
class SampleApplication

@RestController
class ValidationController {

    @PostMapping("/validation", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun sampleValidateEndpoint(@RequestBody request: RequestDto): ResponseDto =
        when {
            request.text == null -> ResponseDto(1, "Null text") /* 1 */
            request.text.isBlank() -> ResponseDto(2, "Blank text") /* 2 */
            else -> ResponseDto(0, "Ok") /* 3 */
        }
}