package ru.iopump.qa.sample.service

import retrofit2.Call
import retrofit2.http.*
import ru.iopump.qa.sample.model.Employee

interface EmployeeService {

    @GET("employee/{id}")
    fun get(@Path("id") id: Int): Call<Employee>

    @POST("employee")
    fun create(
        @Header("Authorization") bearer: String,
        @Body employee: Employee
    ): Call<Employee>
}