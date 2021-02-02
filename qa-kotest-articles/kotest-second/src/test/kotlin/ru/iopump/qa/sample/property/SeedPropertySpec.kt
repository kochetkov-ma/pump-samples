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

class SeedPropertySpec : FreeSpec() {

    init {

        "print seed on fail" {
            /*1*/shouldThrow<AssertionError> {
            checkAll<Int> { number ->
                println("#${attempts()} $number")
                /*2*/number.shouldBeGreaterThanOrEqual(0)
            }
        }./*3*/message.shouldContain("Repeat this test by using seed -?\\d+".toRegex())
        }

        "test with seed will generate the same sequence" {
            Arb.int().checkAll(/*4*/ PropTestConfig(1234567890)) { number ->
                /*5*/if (attempts() == 24) number shouldBe 196548668
                if (attempts() == 428) number shouldBe -601350461
                if (attempts() == 866) number shouldBe 1742824805
            }
        }
    }
}