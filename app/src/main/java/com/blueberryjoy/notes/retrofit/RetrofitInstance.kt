package com.blueberryjoy.notes.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    //private var BASE_URL = "http://10.0.2.2:5001/api/"
    //private var BASE_URL = "http://192.168.0.18:5001/api/"
    private var BASE_URL = "http://192.168.0.40:5001/api/"
    //private var BASE_URL = "https://notes.bbjdev.com/api/"

    val api: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .authenticator(TokenAuthenticator())
            .addInterceptor(AuthInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val refresh: ApiRefresh by lazy {
        val refreshClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiRefresh::class.java)
    }
}