package ru.iopump.qa.sample.extension

import io.kotest.core.extensions.SpecExtension
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

@AutoScan
object CustomKotestExtension : TestListener, SpecExtension {

    private val log = LoggerFactory.getLogger(CustomKotestExtension::class.java)

    override val name: String = "CustomKotestExtension"

    override suspend fun intercept(spec: KClass<out Spec>, process: suspend () -> Unit) {
        log.info("[EXT][BEFORE] intercept process $spec")
        process()
        log.info("[EXT][AFTER] intercept process $spec")
    }

    override suspend fun prepareSpec(kclass: KClass<out Spec>) {
        log.info("[BEFORE] prepareSpec $kclass")
    }

    override suspend fun finalizeSpec(kclass: KClass<out Spec>, results: Map<TestCase, TestResult>) {
        log.info("[AFTER] finalizeSpec $kclass")
    }
}