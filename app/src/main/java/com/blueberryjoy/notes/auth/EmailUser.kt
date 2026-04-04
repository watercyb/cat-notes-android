package com.blueberryjoy.notes.auth

import android.app.AlertDialog
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager.changePassword
import com.blueberryjoy.notes.auth.AuthManager.emailLogout
import com.blueberryjoy.notes.utils.Message
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class EmailUser : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var updateButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var avatarImage: ImageView
    private lateinit var changePasswordButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        updateButton = findViewById(R.id.updateButton)
        logoutButton = findViewById(R.id.logoutButton)
        avatarImage = findViewById(R.id.avatarImage)
        changePasswordButton = findViewById(R.id.changePasswordButton)

        emailInput.setText(AuthManager.getEmail())
        nameInput.setText(AuthManager.getUsername())

        updateButton.setOnClickListener {
            updateButton.isEnabled = false
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val view = currentFocus ?: EditText(this@EmailUser) // fallback view
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            nameInput.isEnabled = false

            lifecycleScope.launch {
                val newName = nameInput.text.toString()
                if (newName != AuthManager.getUsername())
                    if (AuthManager.changeUsername(newName)) avatarImage.setImageBitmap(AuthManager.getAvatar())
                nameInput.isEnabled = true
                updateButton.isEnabled = true
            }
        }

        changePasswordButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.email_user_update_password, null)
            val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
            val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)

            val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_password))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.update)) { _, _ ->
                    val oldPassword = etOldPassword.text.toString()
                    val newPassword = etNewPassword.text.toString()

                    if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                        Message.allFieldsRequired()
                    } else {
                        lifecycleScope.launch {
                            changePassword(oldPassword, newPassword)
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .create()

            dialog.show()
        }

        logoutButton.setOnClickListener {
            logoutButton.isEnabled = false
            lifecycleScope.launch {
                if (emailLogout()) finish()
                logoutButton.isEnabled = true
            }
        }

        lifecycleScope.launch {
            avatarImage.setImageBitmap(AuthManager.getAvatar())
        }
    }
}