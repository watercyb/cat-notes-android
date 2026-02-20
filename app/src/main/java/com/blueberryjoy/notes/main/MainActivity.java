package com.blueberryjoy.notes.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.auth.utils.AuthType;
import com.blueberryjoy.notes.auth.Login;
import com.blueberryjoy.notes.auth.AuthManager;
import com.blueberryjoy.notes.auth.utils.OnAuthChangeHandler;
import com.blueberryjoy.notes.auth.utils.Avatar;
import com.blueberryjoy.notes.clerk.ClerkAuth;
import com.blueberryjoy.notes.clerk.OAuthViewModel;
import com.blueberryjoy.notes.editor.Editor;
import com.blueberryjoy.notes.notes.Notes;
import com.blueberryjoy.notes.volley.JsonRequest;
import com.blueberryjoy.notes.volley.SendVolleyRequest;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import com.bumptech.glide.Glide;
import com.clerk.api.sso.OAuthProvider;

import java.util.List;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;


public class MainActivity extends AppCompatActivity {
    String baseUrl = "http://10.0.2.2:5000/api/";
    //String baseUrl="https://notes.bbjdev.com/api/";

    private ImageButton imageButtonSetting;
    private ListView listViewSetting;
    private RecyclerView recyclerView;
    private ImageButton imageButtonAvatar;

    private OAuthViewModel viewModel;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ActivityResultLauncher<Intent> editorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeViewModel();
        initializeAuthManager();
        Notes.initialize();
        initializeListView();
        initializeRecyclerView();
        initializeEditorLauncher();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        Rect rect = new Rect();
        listViewSetting.getGlobalVisibleRect(rect);
        if (!rect.contains(x, y)) {
            imageButtonSetting.getGlobalVisibleRect(rect);
            if (!rect.contains(x, y)) listViewSetting.setVisibility(View.GONE);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void login(String email, String password) {
        if (email == null || password == null) {
            Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_LONG).show();
        }
        String url = baseUrl + "auth/login";

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            JSONError(e);
        }

        JsonRequest request = new JsonRequest(Request.Method.POST, url, jsonBody, response -> {
            Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
        }, error -> {
            if (error.networkResponse != null) {
                int statusCode = error.networkResponse.statusCode;
                String body = new String(error.networkResponse.data);
                try {
                    JSONObject json = new JSONObject(body);
                    String message = json.getString("message");
                    Log.e("testLog", "Error message: " + message);
                    Toast.makeText(MainActivity.this, "(" + statusCode + ") " + message, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    JSONError(e);
                }
            } else {
                Log.w("testLog", error.toString());
                Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_LONG).show();
            }
        });

        queue.add(request);
    }

    private void checkAuth() {
        String url = baseUrl + "auth/check-auth";

        Log.w("testLog", AuthManager.getToken());

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        // Main thread: update TextView safely
        JsonRequest request = new JsonRequest(Request.Method.GET, url, null,
                response -> {
                    Toast.makeText(MainActivity.this, "Valid user", Toast.LENGTH_SHORT).show();
                }, error -> {
            if (error.networkResponse != null) {
                int statusCode = error.networkResponse.statusCode;
                String body = new String(error.networkResponse.data);
                try {
                    JSONObject json = new JSONObject(body);
                    String message = json.getString("message");
                    Log.e("testLog", "Error message: " + message);
                    Toast.makeText(MainActivity.this, "(" + statusCode + ") " + message, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    JSONError(e);
                }
            } else {
                Log.w("testLog", error.toString());
                Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_LONG).show();
            }
        });

        queue.add(request);
    }

    private void gLogin() {
        //viewModel.signInWithOAuth(OAuthProvider.GOOGLE);
        ClerkAuth.INSTANCE.login(OAuthProvider.GOOGLE, new Continuation<>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {
                Log.w("testLog", "gLogin: " + o);
            }
        });
    }

    private void gCheckAuth() {
        SendVolleyRequest.INSTANCE.checkAuth(new Continuation<>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {
            }
        });
    }

    private void JSONError(Exception e) {
        Log.w("testLog", e.toString());
        Toast.makeText(MainActivity.this, "Invalid input (JSON)", Toast.LENGTH_LONG).show();
    }

    private void initializeListView() {
        listViewSetting.setTranslationZ(10);
        List<String> strs = List.of("NEW NOTE", "test", "CHECK_AUTH", "G-LOGIN", "G-CHECK_AUTH", "SETTING");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.listview_item, R.id.textView, strs);
        ViewGroup.LayoutParams params = listViewSetting.getLayoutParams();
        params.height = (int) ((Math.min(arrayAdapter.getCount() * 41, 250) + 15) * MainActivity.this.getResources().getDisplayMetrics().density);
        params.width = (int) (175 * MainActivity.this.getResources().getDisplayMetrics().density);
        listViewSetting.setLayoutParams(params);

        listViewSetting.setAdapter(arrayAdapter);
        listViewSetting.setOnItemClickListener((parent, view, position, id) -> {
            if (parent == null || parent.getItemAtPosition(position) == null) return;
            switch (parent.getItemAtPosition(position).toString()) {
                case "NEW NOTE":
                    int noteId = Notes.create("", "", null, 0);
                    if (noteId != -1)
                        openEditor(noteId);
                    break;
                case "test":
                    Intent intent = new Intent(MainActivity.this, Login.class);
                    startActivity(intent);
                    break;
                case "LOGIN":
                    //Record.this.startActivity(new Intent(Record.this, Notes.class));
                    login("a", "a");
                    break;
                case "CHECK_AUTH":
                    checkAuth();
                    break;
                case "G-LOGIN":
                    gLogin();
                    break;
                case "G-CHECK_AUTH":
                    gCheckAuth();
                    break;
                case "SETTING":
                    break;
                default:
            }
            listViewSetting.setVisibility(View.GONE);
        });
    }

    private void initializeRecyclerView() {
        List<RecyclerViewItem> list = Notes.getList();

        int spanCount = getResources().getInteger(R.integer.grid_span);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL));
        recyclerViewAdapter = new RecyclerViewAdapter(this, list);
        recyclerViewAdapter.setClickListener((view, position) -> {
            openEditor(list.get(position).id);
        });
        recyclerView.setAdapter(recyclerViewAdapter);

    }

    private void openEditor(int id) {
        Intent intent = new Intent(MainActivity.this, Editor.class);
        intent.putExtra("ID", id);
        editorLauncher.launch(intent);
    }

    private void initializeEditorLauncher() {
        editorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    int position = data.getIntExtra("POSITION", recyclerViewAdapter.getItemCount());
                    if (position < 0) {
                        recyclerViewAdapter.notifyItemRemoved(-position - 1);
                    } else if (position < recyclerViewAdapter.getItemCount()) {
                        recyclerViewAdapter.notifyItemChanged(position);
                    }
                }
            }
        });
    }

    private void initializeViews() {
        imageButtonSetting = findViewById(R.id.imageButtonSetting);
        listViewSetting = findViewById(R.id.listViewSetting);
        recyclerView = findViewById(R.id.recyclerView);
        imageButtonAvatar = findViewById(R.id.imageButtonAvatar);

        imageButtonSetting.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction(new Runnable() {
                @Override
                public void run() {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                }
            }).start();

            if (listViewSetting.getVisibility() == View.VISIBLE) {
                listViewSetting.setVisibility(View.GONE);
            } else {
                listViewSetting.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(MainActivity.this).get(OAuthViewModel.class);
    }

    private void initializeAuthManager() {
        AuthManager.initialize(new OnAuthChangeHandler() {
            @Override
            public void updateAvatar() {
                if (AuthManager.type == AuthType.GOOGLE) {
                    String url = ClerkAuth.INSTANCE.getAvatar();
                    if (!url.isEmpty()) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Glide.with(MainActivity.this)
                                        .load(url)
                                        .circleCrop()
                                        .into(imageButtonAvatar);
                            }
                        });
                    }
                } else if (AuthManager.type == AuthType.EMAIL) {
                    String username = AuthManager.getUsername();
                    if (username != null) {
                        int size = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                48,
                                getResources().getDisplayMetrics()
                        );

                        Bitmap bmp = Avatar.createLetterAvatar(username, size);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                imageButtonAvatar.setImageBitmap(bmp);
                            }
                        });
                    }
                }
            }
        });
    }
}