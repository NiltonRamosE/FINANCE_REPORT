package com.nramos.finance_report.data.api

import com.nramos.finance_report.data.api.responses.BaseResponse
import com.nramos.finance_report.data.api.responses.LoginResponse
import com.nramos.finance_report.data.model.request.LoginRequest
import com.nramos.finance_report.data.model.request.RegisterRequest
import com.nramos.finance_report.data.model.request.ReportRequest
import com.nramos.finance_report.data.model.response.CategoryResponse
import com.nramos.finance_report.data.model.response.ModalityResponse
import com.nramos.finance_report.data.model.response.ReportResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<BaseResponse<LoginResponse>>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<BaseResponse<LoginResponse>>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<BaseResponse<Unit>>

    @GET("categories")
    suspend fun getCategories(
        @Header("Authorization") token: String
    ): Response<BaseResponse<List<CategoryResponse>>>

    @POST("categories")
    suspend fun createCategory(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Response<BaseResponse<CategoryResponse>>

    @DELETE("categories/{categoryId}")
    suspend fun deleteCategory(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Long
    ): Response<BaseResponse<Unit>>

    @GET("reports")
    suspend fun getReports(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("type") type: Char? = null
    ): Response<BaseResponse<List<ReportResponse>>>

    @POST("reports")
    suspend fun createReport(
        @Header("Authorization") token: String,
        @Body request: ReportRequest
    ): Response<BaseResponse<ReportResponse>>

    @GET("modalities")
    suspend fun getModalities(
        @Header("Authorization") token: String
    ): Response<BaseResponse<List<ModalityResponse>>>
}