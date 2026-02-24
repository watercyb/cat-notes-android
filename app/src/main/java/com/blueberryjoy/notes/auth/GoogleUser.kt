package com.blueberryjoy.notes.auth

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager.setLogout
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.message.Message
import com.clerk.api.Clerk
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class GoogleUser : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_google_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val googleLogoutButton = findViewById<MaterialButton>(R.id.googleLogoutButton)

        val user = Clerk.user
        val name = user?.firstName + " " + user?.lastName
        nameInput.setText(name)
        emailInput.setText(user?.emailAddresses?.get(0)?.emailAddress)

        googleLogoutButton.setOnClickListener {
            googleLogoutButton.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                if (ClerkAuth.logout()) finish()
                withContext(Dispatchers.Main) {
                    googleLogoutButton.isEnabled = true
                }
            }
        }
    }
}