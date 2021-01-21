package ru.iopump.qa.sample

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.core.test.TestCaseOrder
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
object ProjectConfig : AbstractProjectConfig() {
    private val log = LoggerFactory.getLogger(ProjectConfig::class.java)

    override val specExecutionOrder: SpecExecutionOrder = SpecExecutionOrder.Annotated
    override val testCaseOrder: TestCaseOrder = TestCaseOrder.Sequential
    override val parallelism: Int = 1

    /** -Dkotest.framework.timeout in ms */
    override val timeout = 60.seconds

    /** -Dkotest.framework.invocation.timeout in ms */
    override val invocationTimeout: Long = 10_000

    /**
     * Save execution results to file for [SpecExecutionOrder.FailureFirst] strategy.
     * [io.kotest.core.config.Configuration.specFailureFilePath] = "./.kotest/spec_failures"
     */
    override val writeSpecFailureFile = true

    override fun beforeAll() {
        log.info("[BEFORE PROJECT] beforeAll")
    }

    override fun afterAll() {
        log.info("[AFTER PROJECT] afterAll")
    }
}