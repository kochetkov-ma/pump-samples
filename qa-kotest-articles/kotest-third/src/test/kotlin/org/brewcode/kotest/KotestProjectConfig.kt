package org.brewcode.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension
import io.qameta.allure.restassured.AllureRestAssured
import io.restassured.RestAssured

object KotestProjectConfig : AbstractProjectConfig() {

    override fun extensions() = listOf(SpringExtension)

    override suspend fun beforeProject() = RestAssured.filters(AllureRestAssured())
}