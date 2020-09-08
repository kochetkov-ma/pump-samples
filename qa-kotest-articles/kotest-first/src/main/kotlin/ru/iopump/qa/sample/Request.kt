package ru.iopump.qa.sample

import java.util.*

data class Request(private val uuid: String = UUID.randomUUID().toString())