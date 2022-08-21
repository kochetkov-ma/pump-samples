package org.brewcode.kotest.ext

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.system.SpecSystemExitListener
import io.kotest.extensions.system.SystemExitException
import io.kotest.matchers.shouldBe
import kotlin.system.exitProcess

@DoNotParallelize /* 1 */
internal class KotestSystemExitTest : StringSpec() {
    /* 2 */
    override fun extensions() = listOf(SpecSystemExitListener)

    init {
        "Scenario: testing application try use System.exit" {
            /* 3 */ shouldThrow<SystemExitException> {
                runApplicationWithOutOfMemoryExitCode() /* 4 */
            }.exitCode shouldBe 137 /* 5 */
        }
    }
}

private fun runApplicationWithOutOfMemoryExitCode(): Nothing = exitProcess(137)

