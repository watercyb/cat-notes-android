package com.blueberryjoy.notes.volley

import android.util.Log
import android.widget.Toast
import com.android.volley.toolbox.Volley
import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.clerk.ClerkAuth
import org.json.JSONObject

object SendVolleyRequest {
    var baseUrl: String = "http://10.0.2.2:5000/api"
    //var baseUrl: String="https://notes.bbjdev.com/api"

    suspend fun checkAuth() {
        val res = ClerkAuth.getToken()
        println("Get token: $res")

        val url = "$baseUrl/auth/check-auth"
        val queue = Volley.newRequestQueue((App.getInstance()).getContext())

        val request = object : JsonRequest(
            Method.GET,
            url,
            null,
            { response ->
                val success = response.getString("success")
                val user = response.getString("user")
                Log.w("testLog", "($success) $user")
            },
            { error ->
                error.networkResponse?.let {
                    val statusCode = it.statusCode
                    val body = String(it.data)
                    try {
                        val json = JSONObject(body)
                        val message = json.getString("message")
                        Log.e("testLog", "Error message: ($statusCode) $message")
                    } catch (e: Exception) {
                        onJSONError(e)
                    }
                } ?: run {
                    Log.w("testLog", error.toString())
                }
            }
        ) {}

        queue.add(request)
    }

    private fun onJSONError(e: java.lang.Exception) {
        Log.w("testLog", e.toString())
        Toast.makeText((App.getInstance()).getContext(), "Invalid input (JSON)", Toast.LENGTH_LONG)
            .show()
    }
}