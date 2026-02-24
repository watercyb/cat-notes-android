package com.blueberryjoy.notes.retrofit

import com.blueberryjoy.notes.retrofit.data_class.ApiRes
import com.blueberryjoy.notes.retrofit.data_class.LoginUser
import com.clerk.api.user.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("auth/check-auth")
    suspend fun checkAuth(): Response<ApiRes>

    @POST("auth/login")
    suspend fun emailLogin(
        @Body user: LoginUser
    ): Response<ApiRes>

    @POST("auth/logout")
    suspend fun emailLogout(): Response<ApiRes>
}