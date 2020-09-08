package ru.iopump.qa.sample

import org.slf4j.LoggerFactory

class Client {

    private companion object {
        private val log = LoggerFactory.getLogger(Client::class.java)
    }

    fun send(request: Request) {
        log.info("Send '$request'")
    }

    fun receive(): Response {
        val result = Response(200)
        log.info("Receive '$result'")
        return result
    }
}