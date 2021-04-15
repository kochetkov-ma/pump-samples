package ru.iopump.qa.sample.http

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.StandardCharsets

class BasicAuthInterceptor(private val basic: String) : Interceptor {

    companion object Factory {
        fun create(username: String, password: String): Interceptor =
            BasicAuthInterceptor(Credentials.basic(username, password, StandardCharsets.UTF_8))
    }

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.request().newBuilder()
            .header("Authorization", basic)
            .build()
            .let { chain.proceed(it) }
}