package ru.iopump.kotest

import io.qameta.allure.Step
import org.springframework.stereotype.Component

/**
 * Spring bean definition.
 */
@Component
class AllureStep {

    operator fun String.minus(par: String) = this + par

    /**
     * Step method with allure annotation.
     */
    @Step("Deploy stand {name}")
    fun deployStand(name: String) {
        ""
    }

    /**
     * Step method with allure annotation.
     */
    @Step("Configure stand {name}")
    fun applyConfig(name: String) {

    }

    /**
     * Step method with allure annotation.
     */
    @Step("Send request to {url}")
    fun sendRequest(url: String) {

    }

    /**
     * Step method with allure annotation.
     */
    @Step("Have response with message {message}")
    fun awaitRequest(message: String) = 200
}