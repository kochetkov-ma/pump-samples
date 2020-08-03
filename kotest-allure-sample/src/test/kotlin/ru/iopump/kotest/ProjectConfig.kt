package ru.iopump.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.Listener
import io.kotest.core.spec.AutoScan
import io.kotest.spring.SpringAutowireConstructorExtension
import io.kotest.spring.SpringListener

@AutoScan
object ProjectConfig : AbstractProjectConfig() {
    override fun extensions(): List<Extension> = listOf(SpringAutowireConstructorExtension)
    override fun listeners(): List<Listener> = listOf(SpringListener)
}