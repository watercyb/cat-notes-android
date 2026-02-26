package com.blueberryjoy.notes.auth

import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager.emailLogout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class EmailUser : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var updateButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameInput = findViewById<EditText>(R.id.nameInput)
        emailInput = findViewById<EditText>(R.id.emailInput)
        updateButton = findViewById<MaterialButton>(R.id.updateButton)
        logoutButton = findViewById<MaterialButton>(R.id.logoutButton)

        emailInput.setText(AuthManager.getEmail())
        nameInput.setText(AuthManager.getUsername())

        updateButton.setOnClickListener {
            updateButton.isEnabled = false
            lifecycleScope.launch {

                updateButton.isEnabled = true
            }
        }

        logoutButton.setOnClickListener {
            logoutButton.isEnabled = false
            lifecycleScope.launch {
                if (emailLogout()) finish()
                logoutButton.isEnabled = true
            }
        }
    }

    private fun update() {

    }

    private fun logout() {

    }
}