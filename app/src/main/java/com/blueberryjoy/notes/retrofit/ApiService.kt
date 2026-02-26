package com.blueberryjoy.notes.retrofit

import com.blueberryjoy.notes.retrofit.data_class.ApiRes
import com.blueberryjoy.notes.retrofit.data_class.ApiReq
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("auth/check-auth")
    suspend fun checkAuth(): Response<ApiRes>

    @POST("auth/login")
    suspend fun emailLogin(
        @Body user: ApiReq
    ): Response<ApiRes>

    @POST("auth/logout")
    suspend fun emailLogout(): Response<ApiRes>

    @POST("note/create")
    suspend fun createNote(
        @Body note: ApiReq
    ): Response<ApiRes>

    @DELETE("note/{mongoId}")
    suspend fun deleteNote(
        @Path("mongoId") mongoId: String
    ): Response<ApiRes>

    @POST("note/update")
    suspend fun updateNote(
        @Body note: ApiReq
    ): Response<ApiRes>

    @GET("note/getNotesIdAndVersion")
    suspend fun getNotesIdAndVersion(): Response<ApiRes>

    @GET("note/{mongoId}")
    suspend fun getNote(
        @Path("mongoId") mongoId: String
    ): Response<ApiRes>
}