package com.blueberryjoy.notes.volley

import android.util.Log
import android.widget.Toast
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.utils.AuthType
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.message.Message
import org.json.JSONObject

object SendVolleyRequest {
    var baseUrl: String = "http://10.0.2.2:5000/api"
    //var baseUrl: String="https://notes.bbjdev.com/api"

    suspend fun checkAuth() {
        if (AuthManager.getType() == AuthType.NULL) return
        if (AuthManager.getType() == AuthType.GOOGLE && !ClerkAuth.getToken()) return

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
            { error -> requestErrorHandler(error) }
        ) {}

        queue.add(request)
    }

    private fun requestErrorHandler(error: VolleyError) {
        if (error.networkResponse != null) {
            val statusCode = error.networkResponse.statusCode
            val body = String(error.networkResponse.data)

            when (statusCode) {
                401 -> {
                    AuthManager.setLogout()
                    Message.show("Login credential expired")
                }

                429 -> {
                    Message.show("Too many requests")
                }

                500 -> {
                    Message.show("Server internal error")
                }
            }

            try {
                val json = JSONObject(body)
                val message = json.getString("message")
                Log.e("testLog", "Error message: ($statusCode) $message")
            } catch (e: Exception) {
                onJSONError(e)
            }
        } else {
            Message.show("Cannot connect to server")
        }
    }

    private fun onJSONError(e: java.lang.Exception) {
        Log.w("testLog", e.toString())
        Toast.makeText((App.getInstance()).getContext(), "Invalid input (JSON)", Toast.LENGTH_LONG)
            .show()
    }
}