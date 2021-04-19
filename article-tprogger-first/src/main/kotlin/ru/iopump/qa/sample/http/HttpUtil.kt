package ru.iopump.qa.sample.http

import io.qameta.allure.okhttp3.AllureOkHttp3
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object HttpUtil {
    private val log = LoggerFactory.getLogger(HttpUtil::class.java)

    val slf4jInterceptor = HttpLoggingInterceptor(log::info).apply {
        when {
            log.isTraceEnabled -> setLevel(HttpLoggingInterceptor.Level.BODY)
            log.isDebugEnabled -> setLevel(HttpLoggingInterceptor.Level.HEADERS)
            log.isInfoEnabled -> setLevel(HttpLoggingInterceptor.Level.BASIC)
            else -> setLevel(HttpLoggingInterceptor.Level.NONE)
        }
    }

    val allureInterceptor: Interceptor = AllureOkHttp3()

    val jsonConverter: Converter.Factory = JacksonConverterFactory.create()

    private fun createOkHttpClient(interceptors: Collection<Interceptor>) =
        OkHttpClient.Builder().apply {
            interceptors.forEach { interceptor -> addInterceptor(interceptor) }
        }.build()

    fun createRetrofit(
        baseUrl: String,
        converter: Converter.Factory,
        interceptors: Collection<Interceptor>
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(createOkHttpClient(interceptors))
        .addConverterFactory(converter)
        .build()
}