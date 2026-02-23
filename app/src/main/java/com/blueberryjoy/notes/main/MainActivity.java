package com.blueberryjoy.notes.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.message.Message;
import com.blueberryjoy.notes.auth.Login;
import com.blueberryjoy.notes.auth.AuthManager;
import com.blueberryjoy.notes.utils.ListViewItem;
import com.blueberryjoy.notes.utils.OnAuthChangeHandler;
import com.blueberryjoy.notes.clerk.ClerkAuth;
import com.blueberryjoy.notes.clerk.OAuthViewModel;
import com.blueberryjoy.notes.editor.Editor;
import com.blueberryjoy.notes.notes.Notes;
import com.blueberryjoy.notes.volley.SendVolleyRequest;

import org.jetbrains.annotations.NotNull;

import com.clerk.api.Clerk;

import java.util.ArrayList;
import java.util.List;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;


public class MainActivity extends AppCompatActivity {
    private ImageButton imageButtonSetting;
    private ListView listViewSetting;
    private RecyclerView recyclerView;
    private ImageButton imageButtonAvatar;

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
        getWindow().setNavigationBarColor(getColor(R.color.background));

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

    private void initializeListView() {
        listViewSetting.setTranslationZ(10);
        List<ListViewItem> list = new ArrayList<>();

        list.add(new ListViewItem("NEW NOTE", () -> {
            int noteId = Notes.create("", "", null, 0);
            if (noteId != -1) openEditor(noteId);
        }));
        list.add(new ListViewItem("LOGIN", () -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        }));
        list.add(new ListViewItem("CHECK_AUTH", () -> {
            gCheckAuth();
        }));
        list.add(new ListViewItem("G-LOGOUT", () -> {
            ClerkAuth.INSTANCE.logout(new Continuation<>() {
                @NotNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NotNull Object o) {
                    Message.logW("G-LOGOUT: " + Clerk.INSTANCE.getUser());
                    AuthManager.setLogout();
                }
            });
        }));
        list.add(new ListViewItem("TEST", () -> {
        }));
        list.add(new ListViewItem("SETTING", () -> {
        }));

        ListViewAdapter listViewAdapter = new ListViewAdapter(this, list);
        ViewGroup.LayoutParams params = listViewSetting.getLayoutParams();
        params.height = (int) ((Math.min(listViewAdapter.getCount() * 41, 250) + 15) * MainActivity.this.getResources().getDisplayMetrics().density);
        params.width = (int) (175 * MainActivity.this.getResources().getDisplayMetrics().density);
        listViewSetting.setLayoutParams(params);

        listViewSetting.setAdapter(listViewAdapter);
        listViewSetting.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= list.size())
                return;
            list.get(position).onClick();
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

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                Notes.swap(from, to);
                recyclerViewAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };

        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);

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
        new ViewModelProvider(MainActivity.this).get(OAuthViewModel.class);
    }

    private void initializeAuthManager() {
        AuthManager.initialize(new OnAuthChangeHandler() {
            @Override
            public void updateAvatar(Bitmap image) {
                if (image != null) {
                    new Handler(Looper.getMainLooper()).post(() -> imageButtonAvatar.setImageBitmap(image));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> imageButtonAvatar.setImageResource(R.mipmap.avatar));
                }
            }
        });
    }
}