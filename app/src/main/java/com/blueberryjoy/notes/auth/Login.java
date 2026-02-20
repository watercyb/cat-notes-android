package com.blueberryjoy.notes.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.main.MainActivity;
import com.blueberryjoy.notes.volley.JsonRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class Login extends AppCompatActivity {
    String baseUrl = "http://10.0.2.2:5000/api/";
    //String baseUrl="https://notes.bbjdev.com/api/";

    TextInputEditText emailInput;
    TextInputEditText passwordInput;
    MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
    }

    private void login() {
        if (emailInput.getText() == null || passwordInput.getText() == null) {
            Toast.makeText(Login.this, "Invalid input", Toast.LENGTH_LONG).show();
            return;
        }
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        String url = baseUrl + "auth/login";

        RequestQueue queue = Volley.newRequestQueue(Login.this);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            JSONError(e);
        }

        JsonRequest request = new JsonRequest(Request.Method.POST, url, jsonBody, response -> {
            Toast.makeText(Login.this, "Logged in", Toast.LENGTH_SHORT).show();
            try {
                boolean success = response.getBoolean("success");
                if (success) {
                    JSONObject user = response.getJSONObject("user");
                    String username = user.getString("name");
                    AuthManager.setEmailLogin(username);
                }
            } catch (JSONException e) {
                JSONError(e);
            }
            Log.w("testLog", response.toString());
            finish();
        }, error -> {
            if (error.networkResponse != null) {
                int statusCode = error.networkResponse.statusCode;
                String body = new String(error.networkResponse.data);
                try {
                    JSONObject json = new JSONObject(body);
                    String message = json.getString("message");
                    Log.e("testLog", "Error message: " + message);
                    Toast.makeText(Login.this, "(" + statusCode + ") " + message, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    JSONError(e);
                }
            } else {
                Log.w("testLog", error.toString());
                Toast.makeText(Login.this, "Network error", Toast.LENGTH_LONG).show();
            }
        });

        queue.add(request);
    }

    private void JSONError(Exception e) {
        Log.w("testLog", e.toString());
        Toast.makeText(Login.this, "Invalid input (JSON)", Toast.LENGTH_LONG).show();
    }
}