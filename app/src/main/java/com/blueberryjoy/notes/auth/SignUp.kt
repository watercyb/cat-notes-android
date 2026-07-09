package com.blueberryjoy.notes.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.main.MainActivity
import com.blueberryjoy.notes.utils.Message
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlin.jvm.java

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usernameInput: TextInputEditText = findViewById(R.id.usernameInput)
        val emailInput: TextInputEditText = findViewById(R.id.emailInput)
        val passwordInput: TextInputEditText = findViewById(R.id.passwordInput)
        val signUpButton: MaterialButton = findViewById(R.id.signUpButton)

        signUpButton.setOnClickListener {
            val name = usernameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Message.show(getString(R.string.all_fields_required))
            } else {
                lifecycleScope.launch {
                    if (AuthManager.emailSignup(name, email, password))
                        toMain()
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    toLogin()
                }
            })
    }

    @Suppress("EmptyMethod")
    override fun onDestroy() {
        super.onDestroy()
    }

    fun toMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    fun toLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}