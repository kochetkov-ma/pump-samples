package ru.iopump.qa.sample.property

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.andNull
import io.kotest.property.exhaustive.boolean
import java.util.concurrent.atomic.AtomicInteger

class UserPropertySpec : FreeSpec() {
    private lateinit var counter: AtomicInteger
    private val num: Int get() = counter.incrementAndGet()
    private val phonePattern: Regex = "\\+7\\(\\d{3}\\)\\d{3}-\\d{2}-\\d{2}".toRegex()

    init {
        beforeTest { counter = AtomicInteger() }

        "phone should match phone pattern" {
            Arb.stringPattern(phonePattern.pattern).checkAll { phone ->
                println("#$num Phone $phone")
                phone.shouldMatch(phonePattern)
            }
        }

        "exhaustive with null" {
            Exhaustive.boolean().andNull().checkAll { name ->
                println("#$num Boolean $name")
                name.shouldBeOneOf(true, false, null)
            }
        }
    }
}