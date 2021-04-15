package ru.iopump.qa.sample.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.iopump.qa.sample.model.Employee

interface EmployeeService {

    @GET("employee/{id}")
    fun get(@Path("id") id: Int): Call<Employee>

    @POST("employee")
    fun create(@Body employee: Employee): Call<Employee>
}