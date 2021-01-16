package ru.iopump.qa.sample.ordering

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FreeSpec

@Order(Int.MAX_VALUE)
class LastSpec : FreeSpec() {
    init {
        "LastSpec-Test" { }
    }
}