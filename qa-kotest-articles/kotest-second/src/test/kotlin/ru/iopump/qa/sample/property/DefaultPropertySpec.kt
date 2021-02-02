package ru.iopump.qa.sample.property

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.checkAll

class DefaultPropertySpec : FreeSpec() {

    init {
        "check 1000 Long numbers" {
            checkAll<Long> { long ->
                val attempt = this.attempts()
                println("#$attempt - $long")
                long.shouldNotBeNull()
            }
        }
    }
}