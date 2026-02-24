package com.blueberryjoy.notes.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager.isLogin
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.clerk.api.sso.OAuthProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {
    var emailInput: TextInputEditText? = null
    var passwordInput: TextInputEditText? = null
    var loginButton: MaterialButton? = null
    var googleLoginButton: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        googleLoginButton = findViewById(R.id.googleLoginButton)

        loginButton!!.setOnClickListener { loginEmail() }

        googleLoginButton!!.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                loginGoogle()
            }
        }
    }

    private fun loginEmail() {
        if (emailInput!!.getText() == null || passwordInput!!.getText() == null) {
            Toast.makeText(this@Login, "Invalid input", Toast.LENGTH_LONG).show()
            return
        }
        loginButton?.isEnabled = false
        googleLoginButton?.isEnabled = false

        lifecycleScope.launch {
            if (AuthManager.emailLogin(
                    emailInput!!.getText().toString(),
                    passwordInput!!.getText().toString()
                )
            ) finish()

            loginButton?.isEnabled = true
            googleLoginButton?.isEnabled = true
        }
    }

    /*
    private fun loginEmail_volley() {
        if (emailInput!!.getText() == null || passwordInput!!.getText() == null) {
            Toast.makeText(this@Login, "Invalid input", Toast.LENGTH_LONG).show()
            return
        }

        loginButton?.isEnabled = false
        googleLoginButton?.isEnabled = false

        val email = emailInput!!.getText().toString()
        val password = passwordInput!!.getText().toString()

        val url = getInstance().getUrl() + "/auth/login"

        val queue = Volley.newRequestQueue(this@Login)

        val jsonBody = JSONObject()
        try {
            jsonBody.put("email", email)
            jsonBody.put("password", password)
        } catch (e: JSONException) {
            JSONError(e)
        }

        val request = JsonRequest(
            Request.Method.POST,
            url,
            jsonBody,
            { response: JSONObject? ->
                Toast.makeText(this@Login, "Logged in", Toast.LENGTH_SHORT).show()
                try {
                    val success = response!!.getBoolean("success")
                    if (success) {
                        val user = response.getJSONObject("user")
                        val username = user.getString("name")
                        setEmailLogin(username)
                    }
                } catch (e: JSONException) {
                    JSONError(e)
                    loginButton?.isEnabled = true
                    googleLoginButton?.isEnabled = true
                }
                Log.w("testLog", response.toString())
                finish()
            },
            { error: VolleyError? ->
                if (error!!.networkResponse != null) {
                    val statusCode = error.networkResponse.statusCode
                    val body = String(error.networkResponse.data)
                    try {
                        val json = JSONObject(body)
                        val message = json.getString("message")
                        Log.e("testLog", "Error message: $message")
                        Toast.makeText(
                            this@Login,
                            "($statusCode) $message",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        JSONError(e)
                    }
                } else {
                    Log.w("testLog", error.toString())
                    Toast.makeText(this@Login, "Network error", Toast.LENGTH_LONG).show()
                }
                loginButton?.isEnabled = true
                googleLoginButton?.isEnabled = true
            })

        queue.add(request)
    }
     */

    private suspend fun loginGoogle() {
        enableGoogleLoginButton(false)
        ClerkAuth.login(OAuthProvider.GOOGLE)
        if (isLogin) finish()
        enableGoogleLoginButton(true)
    }

    private fun enableGoogleLoginButton(isEnable: Boolean) {
        runOnUiThread { // Update your view here (e.g., change text, visibility)
            googleLoginButton!!.isEnabled = isEnable
        }
    }


    private fun JSONError(e: Exception) {
        Log.w("testLog", e.toString())
        Toast.makeText(this@Login, "Invalid input (JSON)", Toast.LENGTH_LONG).show()
    }
}