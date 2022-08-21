package org.brewcode.kotest

data class RequestDto(
    val text: String?
)

data class ResponseDto(
    val code: Int,
    val message: String
)