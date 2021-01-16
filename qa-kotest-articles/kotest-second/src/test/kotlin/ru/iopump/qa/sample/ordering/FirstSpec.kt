package ru.iopump.qa.sample.ordering

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FreeSpec

@Order(Int.MIN_VALUE)
class FirstSpec : FreeSpec() {
    init {
        "FirstSpec-1-Test" { }
        "FirstSpec-2-Test" { }
    }
}