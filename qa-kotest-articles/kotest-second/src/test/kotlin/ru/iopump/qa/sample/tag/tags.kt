package ru.iopump.qa.sample.tag

import io.kotest.core.NamedTag
import io.kotest.core.Tag

/**
 * TAG for annotation @Tag only.
 */
const val LINUX_TAG = "Linux"

/**
 * Name will be class simple name=Windows
 */
object Windows : Tag()

/**
 * Override name to Linux.
 */
object LinuxTag : Tag() {
    override val name: String = LINUX_TAG
}

/**
 * Create [NamedTag] object with name by constructor.
 * Substitute deprecated [io.kotest.core.StringTag]
 */
val regressTag = NamedTag("regress")