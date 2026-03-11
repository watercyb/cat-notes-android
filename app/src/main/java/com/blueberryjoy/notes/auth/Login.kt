package com.blueberryjoy.notes.auth

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager.isLogin
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.utils.Message
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
        val signUpButton: MaterialButton = findViewById(R.id.signUpButton)

        loginButton!!.setOnClickListener { loginEmail() }

        googleLoginButton!!.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                loginGoogle()
            }
        }

        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loginEmail() {
        if (emailInput!!.getText() == null || passwordInput!!.getText() == null) {
            Message.allFieldsRequired()
            return
        }
        loginButton?.isEnabled = false
        googleLoginButton?.isEnabled = false

        lifecycleScope.launch {
            if (AuthManager.emailLogin(
                    emailInput!!.getText().toString(),
                    passwordInput!!.getText().toString()
                )
            ) {
                finish()
            } else {
                val dialog = AlertDialog.Builder(this@Login)
                    .setTitle("Login Error")
                    .setMessage("Invalid email or password")
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Reset Password") { dialogInterface, _ ->
                        lifecycleScope.launch {
                            if (AuthManager.forgotPassword(
                                    emailInput!!.getText().toString()
                                )
                            ) dialogInterface.dismiss()
                        }
                    }
                    .create()

                dialog.show()

                val resetButton: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                resetButton.setTextColor(Color.RED)
            }

            loginButton?.isEnabled = true
            googleLoginButton?.isEnabled = true
        }
    }

    private suspend fun loginGoogle() {
        enableGoogleLoginButton(false)
        ClerkAuth.login(OAuthProvider.GOOGLE)
        if (isLogin) finish()
        enableGoogleLoginButton(true)
    }

    private fun enableGoogleLoginButton(isEnable: Boolean) {
        runOnUiThread {
            googleLoginButton!!.isEnabled = isEnable
        }
    }
}