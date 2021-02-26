package ru.iopump.qa.sample.comment

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldExistInOrder
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.api.SoftAssertions


class AssertJTest : FreeSpec() {
    init {

        "assertj and kotest asserts" - {

            "assertj simple assert" {
                assertThat(1).isCloseTo(2, within(1))
            }

            "kotest simple assert" {
                1 shouldBeInRange 1..2
            }

            val list = listOf(1, 2, 3)
            "assertj collection" {
                assertThat(list).containsExactlyInAnyOrder(1, 2, 3)
            }

            "kotest collection" {
                list shouldContainExactlyInAnyOrder listOf(1, 2, 3)
                list.shouldExistInOrder({ it >= 1 }, { it >= 2 }, { it >= 3 })
            }

            "assertj - assertion error message" {
                shouldThrow<AssertionError> {
                    assertThat(1.0).`as` { "Описание ошибки" }.isEqualTo(2.0)
                }.message.also(::println)
            }

            "kotest - assertion error message" {
                shouldThrow<AssertionError> {
                    1.0 shouldBe 2.0
                }.message.also(::println)
            }

            "assertj - soft assert" {
                shouldThrow<AssertionError> {
                    SoftAssertions().apply {
                        assertThat(1).isEqualTo(2)
                        assertThat(2).isEqualTo(3)
                    }.assertAll()
                }.message.also(::println)
            }

            "kotest - soft assert" {
                shouldThrow<AssertionError> {
                    assertSoftly {
                        1 shouldBe 2
                        2 shouldBe 3
                    }
                }.message.also(::println)
            }
        }
    }
}