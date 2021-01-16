package ru.iopump.qa.sample.ordering

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FreeSpec

@Order(Int.MIN_VALUE + 1)
class SecondSpec : FreeSpec() {
    init {
        "SecondSpec-Test" { }
    }
}