package ru.iopump.qa.sample

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.ExperimentalKotest
import io.kotest.core.listeners.Listener
import io.kotest.extensions.testcontainers.perProject
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import retrofit2.Retrofit
import retrofit2.create
import ru.iopump.qa.sample.http.HttpUtil
import ru.iopump.qa.sample.http.HttpUtil.allureInterceptor
import ru.iopump.qa.sample.http.HttpUtil.jsonConverter
import ru.iopump.qa.sample.http.HttpUtil.slf4jInterceptor
import ru.iopump.qa.sample.service.EmployeeService
import sun.misc.Unsafe
import java.lang.reflect.Field

@ExperimentalKotest
@Suppress("MemberVisibilityCanBePrivate")
object RegistryAndProjectConfiguration : AbstractProjectConfig() {

    override val parallelism: Int = 2

    private val wiremockVersion: String = System.getProperty("wiremock.version")

    val GenericContainer<*>.containerUrl: String
        get() = "http://$containerIpAddress:$firstMappedPort"

    val wiremockContainer = GenericContainer<Nothing>(
        "rodolpheche/wiremock:$wiremockVersion"
    ).apply {
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

    val retrofitJson: Retrofit by lazy {
        HttpUtil.createRetrofit(
            wiremockContainer.containerUrl, jsonConverter,
            listOf(slf4jInterceptor, allureInterceptor)
        )
    }

    val employeeService: EmployeeService by lazy { retrofitJson.create() }

    override fun listeners(): List<Listener> = listOf(
        wiremockContainer.perProject("wiremock")
    )

    init {
        /* Suppress 'Illegal reflective access WARNING' */
        val theUnsafe: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        val u: Unsafe = theUnsafe.get(null) as Unsafe
        val cls = Class.forName("jdk.internal.module.IllegalAccessLogger")
        val logger: Field = cls.getDeclaredField("logger")
        u.putObjectVolatile(cls, u.staticFieldOffset(logger), null)
    }
}