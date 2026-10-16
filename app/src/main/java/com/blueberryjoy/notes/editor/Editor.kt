package com.blueberryjoy.notes.editor

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.blueberryjoy.notes.notes.SyncQueue
import com.blueberryjoy.notes.sqlite.SQLite
import com.blueberryjoy.notes.utils.ClickAnimation
import com.blueberryjoy.notes.utils.Crypto
import com.blueberryjoy.notes.utils.FontManager
import com.blueberryjoy.notes.utils.Message
import com.blueberryjoy.notes.utils.RecyclerViewItem
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

class Editor : AppCompatActivity() {
    lateinit var frameLayoutBottom: FrameLayout
    lateinit var scrollView: ScrollView
    lateinit var editTextTitle: EditText
    lateinit var editTextContent: EditText
    lateinit var imageButtonBack: ImageButton
    lateinit var imageButtonRemove: ImageButton
    lateinit var toolbar: LinearLayout
    lateinit var frameLayoutNote: FrameLayout
    lateinit var imageButtonClone: ImageButton
    lateinit var imageButtonPassword: ImageButton
    lateinit var imageButtonUndo: ImageButton
    lateinit var imageButtonRedo: ImageButton
    lateinit var frameLayoutTitle: FrameLayout
    lateinit var viewContentPadding: View

    private var id: Int = -1
    private var title: String = ""
    private var content: String = ""
    private var isSaved = false
    private var originContent: String? = null

    private var note: RecyclerViewItem? = null
    private var salt: ByteArray? = null
    private var key: SecretKeySpec? = null

    private val viewModel: EditorViewModel by viewModels()

    lateinit var undoRedoHelperContent: UndoRedoHelper
    lateinit var undoRedoHelperTitle: UndoRedoHelper

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
                toolbar.visibility = View.VISIBLE
                viewContentPadding.visibility=View.VISIBLE

                val totalImeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val actualKeyboardHeight = max(0, totalImeBottom - navBarBottom)
                frameLayoutBottom.layoutParams.height = actualKeyboardHeight
                frameLayoutBottom.post { frameLayoutBottom.requestLayout() }
            } else {
                editTextTitle.clearFocus()
                editTextContent.clearFocus()
                toolbar.visibility = View.GONE
                viewContentPadding.visibility=View.GONE

                frameLayoutBottom.layoutParams.height = 0
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

        if (viewModel.note != null) {
            id = viewModel.id
            title = viewModel.title
            content = viewModel.content
            isSaved = viewModel.isSaved
            originContent = viewModel.originContent
            note = viewModel.note
            salt = viewModel.salt
            key = viewModel.key

            if (note!!.isEncrypted && originContent == null) showInputPassword()
        } else {
            val intent = getIntent()
            id = intent.getIntExtra("ID", -1)
            val idx = getNoteById(id)
            if (idx == -1) {
                Message.show(getInstance().getContext().getString(R.string.note_not_found))
                isSaved = true
                finish()
                return
            }
            note = Notes.activeNotes[idx]
            title = note!!.title
            if (!note!!.isEncrypted) {
                content = note!!.content
                originContent = content
            } else {
                showInputPassword()
            }
        }

        initializeViews()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }

    public override fun onDestroy() {
        if (note != null && (!note!!.isEncrypted || originContent != null) && !isSaved) checkAndSave()
        super.onDestroy()
    }

    public override fun onPause() {
        super.onPause()

        viewModel.id = id
        viewModel.title = title
        viewModel.content = content
        viewModel.isSaved = isSaved
        viewModel.originContent = originContent
        viewModel.note = note
        viewModel.salt = salt
        viewModel.key = key
    }

    private fun saveAndFinish() {
        checkAndSave()
        getOnNoteChangeHandler().onChange(note!!)
        isSaved = true
        finish()
    }

    private fun checkAndSave() {
        if (note!!.title != title || originContent != content) {
            note!!.title = title
            note!!.content =
                if (!note!!.isEncrypted) content else Crypto.encrypt(salt!!, key!!, content)
            note!!.version++
            SQLite.saveNote(note!!)
            if (note!!.userId == getUserId()) SyncQueue.add(note!!)
        }
    }

    private fun initializeViews() {
        scrollView = findViewById(R.id.scrollView)
        frameLayoutBottom = findViewById(R.id.frameLayoutBottom)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        imageButtonBack = findViewById(R.id.imageButtonBack)
        imageButtonRemove = findViewById(R.id.imageButtonRemove)
        toolbar = findViewById(R.id.toolbar)
        frameLayoutNote = findViewById(R.id.frameLayoutNote)
        imageButtonClone = findViewById(R.id.imageButtonClone)
        imageButtonPassword = findViewById(R.id.imageButtonPassword)
        imageButtonUndo = findViewById(R.id.imageButtonUndo)
        imageButtonRedo = findViewById(R.id.imageButtonRedo)
        frameLayoutTitle = findViewById(R.id.frameLayoutTitle)
        viewContentPadding = findViewById(R.id.viewContentPadding)

        editTextTitle.setText(title)
        FontManager.apply(editTextTitle)
        editTextContent.setText(content)
        FontManager.apply(editTextContent)

        undoRedoHelperTitle = UndoRedoHelper(editTextTitle, imageButtonUndo, imageButtonRedo)
        undoRedoHelperContent = UndoRedoHelper(editTextContent, imageButtonUndo, imageButtonRedo)

        editTextTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                title = editable.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }
        })
        editTextTitle.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                undoRedoHelperTitle.checkButton()
        }

        editTextContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                content = editable.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }
        })
        editTextContent.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                undoRedoHelperContent.checkButton()
        }

        imageButtonBack.setOnClickListener {
            //view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
            //view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            //}.start()
            saveAndFinish()
        }
        ClickAnimation.set(imageButtonBack)

        imageButtonRemove.setOnClickListener {
            //view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
            //view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            //}.start()
            showDeleteNote()
        }
        ClickAnimation.set(imageButtonRemove)

        imageButtonClone.setOnClickListener {
            //view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
            //view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            //}.start()
            duplicate()
            Message.show(getString(R.string.copy_created))
            finish()
        }
        ClickAnimation.set(imageButtonClone)

        imageButtonPassword.setOnClickListener {
            //view!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction {
            //view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            //}.start()
            showChangePassword()
        }
        ClickAnimation.set(imageButtonPassword)

        if (!note!!.isEncrypted) {
            val userId = getUserId()
            if (userId.isNotEmpty() && userId != note!!.userId) imageButtonClone.visibility =
                View.VISIBLE
        } else {
            if (AuthManager.isOwner(note!!.userId)) imageButtonPassword.visibility = View.VISIBLE
        }

        imageButtonUndo.setOnClickListener {
            if (editTextTitle.hasFocus()) {
                undoRedoHelperTitle.undo()
            } else if (editTextContent.hasFocus()) {
                undoRedoHelperContent.undo()
            }
        }
        ClickAnimation.set(imageButtonUndo)

        imageButtonRedo.setOnClickListener {
            if (editTextTitle.hasFocus()) {
                undoRedoHelperTitle.redo()
            } else if (editTextContent.hasFocus()) {
                undoRedoHelperContent.redo()
            }
        }
        ClickAnimation.set(imageButtonRedo)
    }

    fun showInputPassword() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_editor_password, null)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputPassword)

        val dialog = AlertDialog.Builder(this).setCancelable(false).setView(dialogView).create()

        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        inputPassword.post {
            inputPassword.requestFocus()
            val controller = WindowCompat.getInsetsController(window, inputPassword)
            controller.show(WindowInsetsCompat.Type.ime())
        }
        inputPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                dialogView.findViewById<Button>(R.id.buttonPositive).performClick()
                true
            } else {
                false
            }
        }
        dialogView.findViewById<Button>(R.id.buttonNeutral).setOnClickListener {
            deleteAndClose()
        }
        dialogView.findViewById<Button>(R.id.buttonNegative).setOnClickListener {
            isSaved = true
            finish()
        }
        dialogView.findViewById<Button>(R.id.buttonPositive).setOnClickListener {
            try {
                val (salt, key, content) = Crypto.decrypt(
                    inputPassword.text.toString(), note!!.content
                )
                this.salt = salt
                this.key = key
                this.content = content
                originContent = content
                editTextContent.setText(content)
                dialog.dismiss()
            } catch (_: Exception) {
                Message.showWithDelay(getString(R.string.incorrect_password))
                isSaved = true
                finish()
            }
        }

        dialog.show()
    }

    fun showChangePassword() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_editor_change_password, null)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputPassword)
        val confirmPassword = dialogView.findViewById<EditText>(R.id.confirmPassword)

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        inputPassword.post {
            inputPassword.requestFocus()

            val controller = WindowCompat.getInsetsController(window, inputPassword)
            controller.show(WindowInsetsCompat.Type.ime())
        }

        dialogView.findViewById<Button>(R.id.buttonPositive).setOnClickListener {
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
                note!!.content = encryptedContent
                originContent = null
                note!!.version++
                SQLite.saveNote(note!!)
                Message.show(getString(R.string.password_updated))

                dialog.dismiss()
            } catch (_: Exception) {
                Message.show(getString(R.string.unable_update_password))
            }
        }

        dialogView.findViewById<Button>(R.id.buttonNegative).setOnClickListener {
            dialog.dismiss()
        }

        confirmPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                dialogView.findViewById<Button>(R.id.buttonPositive).performClick()
                true
            } else {
                false
            }
        }

        dialog.show()
    }

    fun deleteAndClose() {
        Notes.moveToTrashCan(note!!)
        Message.showWithDelay(getString(R.string.note_to_trash))
        isSaved = true
        finish()
    }

    fun duplicate() {
        Notes.create(note!!)
    }

    fun showDeleteNote() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_editor_delete, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialogView.findViewById<Button>(R.id.dialog_positive).setOnClickListener {
            deleteAndClose()
        }
        dialogView.findViewById<Button>(R.id.dialog_negative).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}