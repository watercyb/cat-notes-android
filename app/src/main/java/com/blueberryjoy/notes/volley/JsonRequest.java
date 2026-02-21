package com.blueberryjoy.notes.volley;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.blueberryjoy.notes.BuildConfig;
import com.blueberryjoy.notes.auth.AuthManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JsonRequest extends JsonObjectRequest {

    public JsonRequest(
            int method,
            String url,
            JSONObject body,
            Response.Listener<JSONObject> listener,
            Response.ErrorListener errorListener
    ) {
        super(method, url, body, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", AuthManager.getToken());
        headers.put("X-Client-Type", "android-app");
        headers.put("X-App-Version", BuildConfig.VERSION_NAME);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        return super.parseNetworkResponse(response);
    }
}