package com.blueberryjoy.notes.editor

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthManager.getUserId
import com.blueberryjoy.notes.notes.Notes
import com.blueberryjoy.notes.notes.Notes.getNoteById
import com.blueberryjoy.notes.notes.Notes.getOnNoteChangeHandler
import com.blueberryjoy.notes.notes.Notes.list
import com.blueberryjoy.notes.notes.SyncQueue
import com.blueberryjoy.notes.sqlite.SQLite
import com.blueberryjoy.notes.utils.Crypto
import com.blueberryjoy.notes.utils.Message
import com.blueberryjoy.notes.utils.RecyclerViewItem
import javax.crypto.spec.SecretKeySpec

class Editor : AppCompatActivity() {
    lateinit var frameLayoutBottom: FrameLayout
    lateinit var scrollView: ScrollView
    lateinit var editTextTitle: EditText
    lateinit var editTextContent: EditText
    lateinit var imageButtonBack: ImageButton
    lateinit var imageButtonRemove: ImageButton
    lateinit var frameLayoutTitle: FrameLayout
    lateinit var frameLayoutNote: FrameLayout
    lateinit var imageButtonClone: ImageButton
    lateinit var imageButtonPassword: ImageButton

    private var id: Int = -1
    private var title: String = ""
    private var content: String = ""
    private var isSaved = false
    private var originContent: String? = null

    private lateinit var note: RecyclerViewItem
    private lateinit var salt: ByteArray
    private lateinit var key: SecretKeySpec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_editor)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            //Resize edittext Keyboard shows
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (isKeyboardVisible) {
                val decorView = window.decorView
                val rect = Rect()
                decorView.getWindowVisibleDisplayFrame(rect)

                val params = frameLayoutBottom.layoutParams
                params.height =
                    scrollView.height + editTextTitle.height + imageButtonBack.height + frameLayoutTitle.height + frameLayoutNote.height - rect.height()

                frameLayoutBottom.post { frameLayoutBottom.requestLayout() }
            } else {
                val params = frameLayoutBottom.layoutParams
                params.height = 0

                frameLayoutBottom.post { frameLayoutBottom.requestLayout() }
            }
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        } else {
            @Suppress("DEPRECATION")
            window.navigationBarColor = getColor(R.color.background)
        }


        val intent = getIntent()
        id = intent.getIntExtra("ID", -1)
        val idx = getNoteById(id)
        if (idx == -1) {
            Message.show(getInstance().getContext().getString(R.string.note_not_found))
            isSaved = true
            finish()
        }
        note = list[idx]
        title = note.title
        if (!note.isEncrypted) {
            content = note.content
            originContent = content
        } else {
            showInputPassword()
        }

        initializeViews()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }

    public override fun onDestroy() {
        if (!isSaved) checkAndSave()
        super.onDestroy()
    }

    private fun saveAndFinish() {
        checkAndSave()
        getOnNoteChangeHandler().onChange(note)
        isSaved = true
        finish()
    }

    private fun checkAndSave() {
        if (note.title != title || originContent != content) {
            note.title = title
            note.content = if (!note.isEncrypted) content else Crypto.encrypt(salt, key, content)
            note.version++
            SQLite.saveNote(note)
            if (note.userId == getUserId()) SyncQueue.add(note)
        }
    }

    private fun initializeViews() {
        scrollView = findViewById(R.id.scrollView)
        frameLayoutBottom = findViewById(R.id.frameLayoutBottom)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        imageButtonBack = findViewById(R.id.imageButtonBack)
        imageButtonRemove = findViewById(R.id.imageButtonRemove)
        frameLayoutTitle = findViewById(R.id.frameLayoutTitle)
        frameLayoutNote = findViewById(R.id.frameLayoutNote)
        imageButtonClone = findViewById(R.id.imageButtonClone)
        imageButtonPassword = findViewById(R.id.imageButtonPassword)

        editTextTitle.setText(title)
        editTextContent.setText(content)


        editTextTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                title = editable.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }
        })

        editTextContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                content = editable.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }
        })

        imageButtonBack.setOnClickListener { view: View? ->
            view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
            saveAndFinish()
        }

        imageButtonRemove.setOnClickListener { view: View? ->
            view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
            deleteAndClose()
        }

        imageButtonClone.setOnClickListener { view: View? ->
            view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
            duplicate()
            Message.show(getString(R.string.copy_created))
            finish()
        }

        imageButtonPassword.setOnClickListener { view: View? ->
            view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
            showChangePassword()
        }

        if (!note.isEncrypted) {
            val userId = getUserId()
            if (!userId.isEmpty() && userId != note.userId) imageButtonClone.visibility =
                View.VISIBLE
        } else {
            if (AuthManager.isOwner(note.userId)) imageButtonPassword.visibility = View.VISIBLE
        }
    }

    fun showInputPassword() {
        val dialogView = layoutInflater.inflate(R.layout.editor_password, null)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputPassword)

        val dialog = AlertDialog.Builder(this).setTitle(getString(R.string.password_required))
            .setView(dialogView)
            .setCancelable(false).setPositiveButton(getString(R.string.unlock)) { _, _ ->
                try {
                    val (salt, key, content) = Crypto.decrypt(
                        inputPassword.text.toString(),
                        note.content
                    )
                    this.salt = salt
                    this.key = key
                    this.content = content
                    originContent = content
                    editTextContent.setText(content)
                } catch (_: Exception) {
                    Message.show(getString(R.string.incorrect_password))
                    isSaved = true
                    finish()
                }
            }.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                isSaved = true
                finish()
            }.setNeutralButton(getString(R.string.delete)) { _, _ ->
                deleteAndClose()
            }.create()

        dialog.setOnShowListener {
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setTextColor(Color.RED)
        }

        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        inputPassword.post {
            inputPassword.requestFocus()
            //val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            //imm.showSoftInput(inputPassword, InputMethodManager.SHOW_IMPLICIT)

            val controller = WindowCompat.getInsetsController(window, inputPassword)
            controller.show(WindowInsetsCompat.Type.ime())
        }

        dialog.show()

        inputPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }
    }

    fun showChangePassword() {
        val dialogView = layoutInflater.inflate(R.layout.editor_change_password, null)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputPassword)
        val confirmPassword = dialogView.findViewById<EditText>(R.id.confirmPassword)

        val dialog = AlertDialog.Builder(this).setTitle(getString(R.string.update_password))
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.update), null)   // IMPORTANT: null
            .setNegativeButton(getString(R.string.cancel), null).create()

        dialog.setOnShowListener {
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setTextColor(Color.RED)
        }

        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        inputPassword.post {
            inputPassword.requestFocus()
            //val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            //imm.showSoftInput(inputPassword, InputMethodManager.SHOW_IMPLICIT)

            val controller = WindowCompat.getInsetsController(window, inputPassword)
            controller.show(WindowInsetsCompat.Type.ime())
        }

        dialog.show()

        val updateBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        updateBtn.setOnClickListener {
            try {
                val password = inputPassword.text.toString()
                val confirm = confirmPassword.text.toString()
                if (password.isEmpty()) {
                    Message.show(getString(R.string.password_empty))
                    return@setOnClickListener
                }
                if (password != confirm) {
                    Message.show(getString(R.string.password_not_match))
                    return@setOnClickListener
                }

                val (salt, key) = Crypto.getKey(password)
                this.salt = salt
                this.key = key
                val encryptedContent = Crypto.encrypt(salt, key, content)
                note.content = encryptedContent
                originContent = null
                note.version++
                SQLite.saveNote(note)
                Message.show(getString(R.string.password_updated))

                dialog.dismiss()
            } catch (_: Exception) {
                Message.show(getString(R.string.unable_update_password))
            }
        }


        confirmPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }
    }

    fun deleteAndClose() {
        Notes.moveToTrashCan(note)
        Message.show(getString(R.string.note_to_trash))
        isSaved = true
        finish()
    }

    fun duplicate() {
        Notes.create(note)
    }
}