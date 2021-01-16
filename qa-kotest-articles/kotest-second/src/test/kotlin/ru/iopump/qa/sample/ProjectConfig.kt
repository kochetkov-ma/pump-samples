package ru.iopump.qa.sample

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import org.slf4j.LoggerFactory

object ProjectConfig : AbstractProjectConfig() {
    private val log = LoggerFactory.getLogger(ProjectConfig::class.java)
    override val specExecutionOrder = SpecExecutionOrder.Annotated

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