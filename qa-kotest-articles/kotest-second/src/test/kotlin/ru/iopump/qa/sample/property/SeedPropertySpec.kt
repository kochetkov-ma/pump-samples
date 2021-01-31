package ru.iopump.qa.sample.property

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.util.concurrent.atomic.AtomicInteger

class SeedPropertySpec : FreeSpec() {
    private lateinit var counter: AtomicInteger
    private val num: Int get() = counter.incrementAndGet()

    init {
        beforeTest { counter = AtomicInteger() }

        "print seed on fail" {
            shouldThrow<AssertionError> {
                Arb.int().checkAll { number ->
                    println("#$num $number")
                    number.shouldBeGreaterThanOrEqual(0)
                }
            }.message.shouldContain("Repeat this test by using seed -?\\d+".toRegex())
        }

        "test with seed will generate the same sequence" {
            Arb.int().checkAll(PropTestConfig(1234567890)) { number ->
                println("#$num $number")
                if (counter.get() == 25) number shouldBe 196548668
                if (counter.get() == 429) number shouldBe -601350461
                if (counter.get() == 867) number shouldBe 1742824805
            }
        }
    }
}