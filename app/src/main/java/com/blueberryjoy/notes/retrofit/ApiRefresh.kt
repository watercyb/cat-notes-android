package com.blueberryjoy.notes.retrofit

import com.blueberryjoy.notes.retrofit.data_class.ApiReq
import com.blueberryjoy.notes.retrofit.data_class.ApiRes
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiRefresh {
    @POST("auth/refresh")
    suspend fun refresh(@Body user: ApiReq): Response<ApiRes>
}