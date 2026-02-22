package com.blueberryjoy.notes

import android.app.Application
import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.Clerk.publishableKey

class App : Application() {
    val publishableKey: String = "pk_test_b3Blbi1ib2FyLTc0LmNsZXJrLmFjY291bnRzLmRldiQ"
    //val publishableKey: String="pk_live_Y2xlcmsubm90ZXMuYmJqZGV2LmNvbSQ"

    var baseUrl: String = "http://10.0.2.2:5000/api"
    //var baseUrl: String = "http://192.168.0.18:5000/api"
    //var baseUrl: String="https://notes.bbjdev.com/api"

    companion object {
        private lateinit var instance: App

        fun getInstance(): App = instance
    }

    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(
            context = this, publishableKey = publishableKey
        )
        instance = this
    }

    fun getContext(): Context = this

    fun getUrl(): String = baseUrl
}