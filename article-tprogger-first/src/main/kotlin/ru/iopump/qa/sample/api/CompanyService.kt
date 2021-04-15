package ru.iopump.qa.sample.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import ru.iopump.qa.sample.model.Company
import ru.iopump.qa.sample.model.Employee

interface CompanyService {
    @GET("company/{id}")
    fun get(
        @Header("Authorization") bearer: String,
        @Path("id") companyId: Int
    ): Call<Company>

    @GET("company/{id}/employee")
    fun getEmployeeCollection(@Path("id") companyId: Int): Call<Collection<Employee>>
}