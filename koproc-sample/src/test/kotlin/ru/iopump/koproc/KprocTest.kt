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