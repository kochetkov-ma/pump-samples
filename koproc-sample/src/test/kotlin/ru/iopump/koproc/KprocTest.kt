package ru.iopump.koproc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.assertThrows
import java.io.IOException

open class DockerComposeTest : StringSpec() {

    init {

        "Get java version" {
            val result = "java -version".startCommand()
            println(result)
        }

        "Run bad command and throw postponed exception" {
            val result = "wat?".startCommand()

            assertThrows<RuntimeException> {
                result.throwOnAnyFailure()
            }
                .cause
                .shouldBeInstanceOf<IOException>()
        }
    }
}


fun main() {
    "%-30s : %s".format("1", "abc").also(::println)
    "%-30s : %s".format("1234567890", "abc").also(::println)
    "%-30s : %s".format("12345678901234567890", "abc").also(::println)
}