package ru.iopump.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.Listener
import io.kotest.core.spec.AutoScan
import io.kotest.spring.SpringAutowireConstructorExtension
import io.kotest.spring.SpringListener

/**
 * Entire Kotest project configuration
 * [project_config](https://github.com/kotest/kotest/blob/master/doc/project_config.md)
 */
@AutoScan
object ProjectConfig : AbstractProjectConfig() {
    /**
     * Extension for constructor injection
     * [constructor-injection](https://github.com/kotest/kotest/blob/master/doc/extensions.md#constructor-injection)
     */
    override fun extensions(): List<Extension> = listOf(SpringAutowireConstructorExtension)

    /**
     * Listener for field injection
     * [field-injection](https://github.com/kotest/kotest/blob/master/doc/extensions.md#field-injection)
     */
    override fun listeners(): List<Listener> = listOf(SpringListener)
}