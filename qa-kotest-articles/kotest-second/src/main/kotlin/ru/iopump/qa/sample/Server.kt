package ru.iopump.qa.sample

import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class Server {
    private companion object {
        private val log = LoggerFactory.getLogger(Server::class.java)
    }

    fun start() {
        log.info("Server started at " + LocalDateTime.now())
    }
}