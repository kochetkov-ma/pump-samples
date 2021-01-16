package ru.iopump.qa.sample.tag

import io.kotest.core.Tag
import io.kotest.core.spec.style.FreeSpec

class WindowsSpec: FreeSpec() {
    /** Override tags method */
    override fun tags(): Set<Tag> = setOf(Windows)

    init {
        "Test for Windows" { }
    }
}