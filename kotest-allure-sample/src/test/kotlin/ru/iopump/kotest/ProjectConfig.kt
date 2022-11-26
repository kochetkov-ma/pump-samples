package ru.iopump.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringExtension


/**
 * Entire Kotest project configuration
 * [project_config](https://github.com/kotest/kotest/blob/master/doc/project_config.md)
 */
object ProjectConfig : AbstractProjectConfig() {
    /**
     * Extension for constructor injection
     * [constructor-injection](https://github.com/kotest/kotest/blob/master/doc/extensions.md#constructor-injection)
     */
    override fun extensions(): List<Extension> = listOf(SpringExtension)
}