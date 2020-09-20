package ru.iopump.qa.sample

import io.kotest.core.config.AbstractProjectConfig
import org.slf4j.LoggerFactory

object ProjectConfig : AbstractProjectConfig() {
    private val log = LoggerFactory.getLogger(ProjectConfig::class.java)

    override fun beforeAll() {
        log.info("[BEFORE PROJECT] beforeAll")
    }

    override fun afterAll() {
        log.info("[AFTER PROJECT] afterAll")
    }
}