package ru.iopump.qa.sample.tag

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec

@Tags(LINUX_TAG)
class LinuxSpec: FreeSpec() {
    init {
        "1-Test for Linux" { }
        "2-Test for Linux and Regress only".config(tags = setOf(regressTag)) { }
    }
}