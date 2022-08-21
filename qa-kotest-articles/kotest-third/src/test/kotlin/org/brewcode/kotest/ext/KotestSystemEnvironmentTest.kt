package org.brewcode.kotest.ext

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.matchers.shouldBe

@DoNotParallelize
internal class KotestSystemEnvironmentTest : StringSpec() {

    /* 1 */
    override fun extensions() =
        listOf(SystemEnvironmentTestListener(/* 2 */mapOf("USERNAME" to "TEST", "OS" to "Astra Linux"), /* 3 */OverrideMode.SetOrOverride))

    init {
        /* 4 */
        println("Before use listener: " + System.getenv("USERNAME"))
        println("Before use listener: " + System.getenv("OS"))

        "Scenario: environment variables should be mocked" {
            /* 5 */
            System.getenv("USERNAME") shouldBe "TEST"
            System.getenv("OS") shouldBe "Astra Linux"
        }
    }
}