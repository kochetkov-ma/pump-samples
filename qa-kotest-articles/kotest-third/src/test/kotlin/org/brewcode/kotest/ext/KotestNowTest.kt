package org.brewcode.kotest.ext

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.time.ConstantNowTestListener
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DoNotParallelize
internal class KotestNowTest : StringSpec() {

    override fun extensions() = listOf(
        /* 1 */ ConstantNowTestListener<LocalDate>(LocalDate.EPOCH),
        /* 2 */ ConstantNowTestListener<LocalTime>(LocalTime.NOON)
    )

    init {
        "Scenario: date and time will be mocked, but dateTime not" {
            /* 3 */
            LocalDate.now() shouldBe LocalDate.EPOCH
            LocalTime.now() shouldBe LocalTime.NOON

            /* 4 */
            val localDateTimeNow = LocalDateTime.now()
            delay(100)
            LocalDateTime.now() shouldBeAfter localDateTimeNow
        }
    }
}