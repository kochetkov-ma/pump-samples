package ru.iopump.qa.sample.ordering

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.TestCaseOrder

@Order(Int.MIN_VALUE)
class TestOrderingSpec : FreeSpec() {
    override fun testCaseOrder(): TestCaseOrder = TestCaseOrder.Lexicographic

    init {
        "FirstSpec-1-Test" { }
        "FirstSpec-2-Test" { }
    }
}