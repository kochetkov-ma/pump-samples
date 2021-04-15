package ru.iopump.qa.sample

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.ExperimentalKotest
import io.kotest.core.listeners.Listener
import io.kotest.extensions.allure.AllureTestReporter
import io.kotest.extensions.testcontainers.perProject
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import retrofit2.Retrofit
import retrofit2.create
import ru.iopump.qa.sample.api.CompanyService
import ru.iopump.qa.sample.api.EmployeeService
import ru.iopump.qa.sample.http.HttpUtil
import ru.iopump.qa.sample.http.HttpUtil.jsonConverter
import ru.iopump.qa.sample.http.HttpUtil.loggingInterceptors

@ExperimentalKotest
@Suppress("MemberVisibilityCanBePrivate")
object StaticProjectConfiguration : AbstractProjectConfig() {

    override val parallelism: Int = 2

    private val wiremockVersion = System.getProperty("wiremock.version")

    val GenericContainer<*>.containerUrl get() = "http://$containerIpAddress:$firstMappedPort"

    val wiremockContainer = GenericContainer<Nothing>("rodolpheche/wiremock:$wiremockVersion")
        .apply {
            withExposedPorts(8080)
            withCommand("--verbose", "--global-response-templating")
            withClasspathResourceMapping("wiremock", "/home/wiremock", BindMode.READ_ONLY)
        }

    val wiremockClient: WireMock by lazy {
        WireMock.create()
            .host(wiremockContainer.containerIpAddress)
            .port(wiremockContainer.firstMappedPort)
            .build()
    }

    val retrofitJson: Retrofit
            by lazy { HttpUtil.createRetrofit(wiremockContainer.containerUrl, jsonConverter, loggingInterceptors) }

    val companyService: CompanyService by lazy { retrofitJson.create() }

    val employeeService: EmployeeService by lazy { retrofitJson.create() }

    val objectMapper = ObjectMapper()

    override fun listeners(): List<Listener> = listOf(
        wiremockContainer.perProject("wiremock"),
        AllureTestReporter()
    )
}