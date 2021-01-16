package ru.iopump.qa.sample

data class Response(
        val code: Int,
        val body: String = "static-body",
)