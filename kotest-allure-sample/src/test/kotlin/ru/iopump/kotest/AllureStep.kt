package ru.iopump.kotest

import io.qameta.allure.Step
import org.springframework.stereotype.Component

@Component
class AllureStep {
    @Step("Deploy stand {name}")
    fun deployStand(name: String) {

    }

    @Step("Configure stand {name}")
    fun applyConfig(name: String) {

    }

    @Step("Send request to {url}")
    fun sendRequest(url: String) {

    }

    @Step("Have response with message {message}")
    fun awaitRequest(message: String) = 200
}