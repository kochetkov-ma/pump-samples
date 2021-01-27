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
        log.info("[BEFORE] spec $kclass")
    }

    override suspend fun finalizeSpec(kclass: KClass<out Spec>, results: Map<TestCase, TestResult>) {
        val specSequentialDuration = results.values.sumOf { it.duration }
        log.info("[AFTER] spec. Spec sequential duration (actual for thread per spec): $specSequentialDuration")
    }

    override suspend fun beforeContainer(testCase: TestCase) {
        log.info("[BEFORE] container ${testCase.displayName}")
    }

    override suspend fun afterContainer(testCase: TestCase, result: TestResult) {
        log.info("[AFTER] container ${testCase.displayName}. Test container duration: ${result.duration} ms")
    }

    override suspend fun beforeTest(testCase: TestCase) {
        log.info("[BEFORE] test ${testCase.displayName}")
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        log.info("[AFTER] test ${testCase.displayName}. Duration: ${result.duration} ms")
    }
}