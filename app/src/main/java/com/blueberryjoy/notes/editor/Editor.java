package com.blueberryjoy.notes.editor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.main.RecyclerViewItem;
import com.blueberryjoy.notes.notes.Notes;
import com.blueberryjoy.notes.sqlite.SQLite;

public class Editor extends AppCompatActivity {
    FrameLayout frameLayoutBottom;
    ScrollView scrollView;
    EditText editTextTitle;
    EditText editTextContent;
    ImageButton imageButtonBack;
    ImageButton imageButtonRemove;
    FrameLayout frameLayoutTitle;
    FrameLayout frameLayoutNote;

    int idx = -1;

    RecyclerViewItem note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            //Resize edittext Keyboard shows
            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (isKeyboardVisible) {
                View decorView = getWindow().getDecorView();
                Rect rect = new Rect();
                decorView.getWindowVisibleDisplayFrame(rect);

                ViewGroup.LayoutParams params = frameLayoutBottom.getLayoutParams();
                params.height = scrollView.getHeight() + editTextTitle.getHeight() + imageButtonBack.getHeight() + frameLayoutTitle.getHeight() + frameLayoutNote.getHeight() - rect.height();

                frameLayoutBottom.post(() -> frameLayoutBottom.requestLayout());
            } else {
                ViewGroup.LayoutParams params = frameLayoutBottom.getLayoutParams();
                params.height = 0;

                frameLayoutBottom.post(() -> frameLayoutBottom.requestLayout());
            }

            return insets;
        });
        getWindow().setNavigationBarColor(getColor(R.color.background));

        Intent intent = getIntent();
        int id = intent.getIntExtra("ID", -1);
        idx = Notes.getNoteByIdx(id);
        if (idx == -1) {
            Toast.makeText(this, "Note cannot be found", Toast.LENGTH_LONG).show();
            finish();
        }
        note = Notes.getList().get(idx);

        initializeViews();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAndFinish();
            }
        });
    }

    @Override
    public void onDestroy() {
        SQLite.saveNote(note);
        super.onDestroy();
    }

    private void saveAndFinish() {
        SQLite.saveNote(note);
        Intent intent = new Intent();
        intent.putExtra("POSITION", idx);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void initializeViews() {
        scrollView = findViewById(R.id.scrollView);
        frameLayoutBottom = findViewById(R.id.frameLayoutBottom);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);
        imageButtonBack = findViewById(R.id.imageButtonBack);
        imageButtonRemove = findViewById(R.id.imageButtonRemove);
        frameLayoutTitle = findViewById(R.id.frameLayoutTitle);
        frameLayoutNote = findViewById(R.id.frameLayoutNote);

        editTextTitle.setText(note.title);
        editTextContent.setText(note.content);


        editTextTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void afterTextChanged(@NonNull Editable editable) {
                note.title = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        editTextContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void afterTextChanged(@NonNull Editable editable) {
                note.content = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        imageButtonBack.setOnClickListener(view -> saveAndFinish());

        imageButtonRemove.setOnClickListener(view -> {
            SQLite.delete(note);
            Notes.remove(note);
            Intent intent = new Intent();
            intent.putExtra("POSITION", -idx - 1);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }
}