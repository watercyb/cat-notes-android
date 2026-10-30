package com.blueberryjoy.notes.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.BuildConfig
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthManager.getType
import com.blueberryjoy.notes.auth.AuthManager.initialize
import com.blueberryjoy.notes.auth.AuthType
import com.blueberryjoy.notes.auth.EmailUser
import com.blueberryjoy.notes.auth.GoogleUser
import com.blueberryjoy.notes.auth.Login
import com.blueberryjoy.notes.clerk.OAuthViewModel
import com.blueberryjoy.notes.editor.Editor
import com.blueberryjoy.notes.notes.Notes
import com.blueberryjoy.notes.notes.OnNoteChangeHandler
import com.blueberryjoy.notes.trash_can.TrashCan
import com.blueberryjoy.notes.utils.Crypto
import com.blueberryjoy.notes.utils.Message
import com.blueberryjoy.notes.utils.RecyclerViewItem
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import androidx.core.net.toUri
import com.blueberryjoy.notes.auth.AuthManager.emailLogout
import com.blueberryjoy.notes.setting.Setting
import com.blueberryjoy.notes.utils.ClickAnimation
import kotlin.jvm.java


class MainActivity : AppCompatActivity() {
    private lateinit var imageButtonSetting: ImageButton
    private lateinit var listViewSetting: ListView
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageButtonAvatar: ImageButton
    private lateinit var viewNotificationDot: View
    private lateinit var emptyView: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private lateinit var recyclerViewAdapter: MainRecyclerViewAdapter

    private lateinit var menuContainer: MaterialCardView
    private val menuItemList: MutableList<ListViewItem> = ArrayList()

    private var lastActiveTime: Long = -1L
    private var resendAvailableAt: Long = 0L
    private var backPressedTime: Long = 0L

    private var syncButton: ListViewItem? = null

    private lateinit var settingLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        } else {
            @Suppress("DEPRECATION")
            window.navigationBarColor = getColor(R.color.background)
        }

        initializeViews()
        initializeAuthManager()
        initializeViewModel()
        initializeNote()
        initializeListView()
        initializeRecyclerView()
        initializeSettingLauncher()
        checkTime()
    }

    public override fun onResume() {
        super.onResume()
        checkTime()
        Message.logW("onResume")
        if (!AuthManager.getIsVerified()) {
            setNotificationDotVisible(View.VISIBLE)
        } else {
            setNotificationDotVisible(View.GONE)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.rawX.toInt()
        val y = ev.rawY.toInt()
        val rect = Rect()
        menuContainer.getGlobalVisibleRect(rect)
        if (!rect.contains(x, y)) {
            imageButtonSetting.getGlobalVisibleRect(rect)
            if (!rect.contains(x, y)) hideMenu()
        }
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initializeListView() {
        menuContainer.translationZ = 10f

        /*
        menuItemList.add(
            ListViewItem(getString(R.string.new_note_menu), {
                val noteId = Notes.create("", "", "", 0, 0, false, AuthManager.getUserId(), false)
                if (noteId != -1) openEditor(noteId)
            }, { return@ListViewItem true })
        )

        menuItemList.add(
            ListViewItem(
                getString(R.string.new_secure_note),
                { showInputPassword() },
                { return@ListViewItem true })
        )
        */

        menuItemList.add(
            ListViewItem(getString(R.string.new_note_menu), {
                val view = layoutInflater.inflate(R.layout.dialog_main_new_note, null)
                val dialog = AlertDialog.Builder(this@MainActivity).setView(view).create()
                view.findViewById<TextView>(R.id.new_note).setOnClickListener {
                    val noteId =
                        Notes.create("", "", "", 0, 0, false, AuthManager.getUserId(), false)
                    if (noteId != -1) openEditor(noteId)
                    dialog.dismiss()
                }
                view.findViewById<TextView>(R.id.new_secure_note).setOnClickListener {
                    dialog.dismiss()
                    showInputPassword()
                }
                view.findViewById<Button>(R.id.dialog_negative).setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
            }, { return@ListViewItem true })
        )

        menuItemList.add(
            ListViewItem(getString(R.string.trash_can_menu), {
                val intent = Intent(this@MainActivity, TrashCan::class.java)
                startActivity(intent)
            }, { return@ListViewItem true })
        )

        menuItemList.add(
            ListViewItem(
                if (AuthManager.getIsShowAllNotes()) getString(R.string.my_notes) else getString(
                    R.string.all_notes
                ), { item ->
                    if (item.title == getString(R.string.all_notes)) {
                        item.title = getString(R.string.my_notes)
                        AuthManager.setIsShowAllNotes(true)
                    } else {
                        item.title = getString(R.string.all_notes)
                        AuthManager.setIsShowAllNotes(false)
                    }
                    recyclerViewAdapter.setVisibleList()
                    recyclerViewAdapter.notifyDataSetChanged()
                    setRecyclerViewEmpty(recyclerViewAdapter.itemCount == 0)
                }, {
                    for (note in Notes.activeNotes) {
                        if (!note.userId.equals(AuthManager.getUserId())) {
                            return@ListViewItem true
                        }
                    }
                    return@ListViewItem false
                })
        )

        syncButton = ListViewItem(getString(R.string.sync_notes), { item ->
            if (item.title == getString(R.string.sync_notes)) {
                item.title = getString(R.string.syncing)

                Handler(Looper.getMainLooper()).postDelayed({
                    syncButton!!.title = getString(R.string.sync_notes)
                }, 5000)

                lifecycleScope.launch {
                    if (Notes.getNotesFromApi()) Message.show(getString(R.string.synced))
                    item.title = getString(R.string.sync_notes)
                }
            }
        }, {
            return@ListViewItem true
        })

        menuItemList.add(syncButton!!)

        menuItemList.add(
            ListViewItem(getString(R.string.setting), {
                val intent = Intent(this@MainActivity, Setting::class.java)
                settingLauncher.launch(intent)
            }, { return@ListViewItem true })
        )

        menuItemList.add(
            ListViewItem(getString(R.string.about), { _ ->
                lifecycleScope.launch {
                    val view = layoutInflater.inflate(R.layout.dialog_main_about, null)
                    val dialog =
                        android.app.AlertDialog.Builder(this@MainActivity).setView(view).create()

                    val version: TextView = view.findViewById(R.id.textViewVersion)
                    version.text = java.lang.String.format(
                        "%s%s%s",
                        getString(R.string.about_version),
                        BuildConfig.VERSION_NAME,
                        if (BuildConfig.DEBUG) " (debug)" else ""
                    )
                    val contact: TextView = view.findViewById(R.id.textViewContact)
                    contact.setOnClickListener { _: View? ->
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = "mailto:blueberryjoy24@gmail.com".toUri()
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Cat Notes")
                        startActivity(intent)
                    }

                    view.findViewById<Button>(R.id.dialog_positive).setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
                }
            }, {
                return@ListViewItem true
            })
        )

        /*
        if (BuildConfig.DEBUG) {
            menuItemList.add(
                ListViewItem("CHECK QUEUE", {
                    SyncQueue.showQueue()
                }, { return@ListViewItem true })
            )

            menuItemList.add(ListViewItem("TEST", { _ ->
            }, { return@ListViewItem true }))
        }
        */
    }

    private fun initializeRecyclerView() {
        val spanCount = resources.getInteger(R.integer.grid_span)
        recyclerView.setLayoutManager(
            StaggeredGridLayoutManager(
                spanCount, StaggeredGridLayoutManager.VERTICAL
            )
        )
        recyclerViewAdapter = MainRecyclerViewAdapter(this, Notes.activeNotes)

        recyclerViewAdapter.setClickListener { _: View?, position: Int ->
            openEditor(recyclerViewAdapter.getIdFromVisibleIdx(position))
        }
        recyclerView.setAdapter(recyclerViewAdapter)

        val callback: ItemTouchHelper.Callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.getBindingAdapterPosition()
                val to = target.getBindingAdapterPosition()
                //Notes.swap(from, to)
                recyclerViewAdapter.swap(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        }

        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)

        if (recyclerViewAdapter.itemCount == 0) setRecyclerViewEmpty(true)
    }

    private fun openEditor(id: Int) {
        val intent = Intent(this@MainActivity, Editor::class.java)
        intent.putExtra("ID", id)
        startActivity(intent)
    }

    private fun initializeViews() {
        imageButtonSetting = findViewById(R.id.imageButtonSetting)
        listViewSetting = findViewById(R.id.listViewSetting)
        recyclerView = findViewById(R.id.recyclerView)
        imageButtonAvatar = findViewById(R.id.imageButtonAvatar)
        viewNotificationDot = findViewById(R.id.viewNotificationDot)
        emptyView = findViewById(R.id.emptyView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        menuContainer = findViewById(R.id.menuContainer)

        imageButtonSetting.setOnClickListener {
            //v!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
            //.withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }.start()
            if (menuContainer.isVisible) {
                hideMenu()
            } else {
                showMenu()
            }
        }
        ClickAnimation.set(imageButtonSetting)
        imageButtonAvatar.setOnClickListener {
            if (!AuthManager.getIsVerified()) {
                showVerifyEmailDialog()
            } else if (getType() == AuthType.EMAIL) {
                val intent = Intent(this@MainActivity, EmailUser::class.java)
                startActivity(intent)
            } else if (getType() == AuthType.GOOGLE) {
                val intent = Intent(this@MainActivity, GoogleUser::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this@MainActivity, Login::class.java)
                startActivity(intent)
            }
        }
        ClickAnimation.set(imageButtonAvatar)

        swipeRefresh.setOnRefreshListener {
            // Trigger your refresh logic
            if (syncButton!!.title == getString(R.string.syncing)) {
                swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }
            syncButton!!.title = getString(R.string.syncing)
            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefresh.isRefreshing = false
                syncButton!!.title = getString(R.string.sync_notes)
            }, 5000)
            lifecycleScope.launch {
                if (Notes.getNotesFromApi()) Message.show(getString(R.string.synced))
                swipeRefresh.isRefreshing = false
                syncButton!!.title = getString(R.string.sync_notes)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            val now = System.currentTimeMillis()
            if (now - backPressedTime < 2000) {
                finish()
            } else {
                backPressedTime = now
                Message.show(getString(R.string.press_back_again_to_exit))
            }
        }
    }

    private fun initializeViewModel() {
        ViewModelProvider(this@MainActivity)[OAuthViewModel::class.java]
    }

    private fun initializeAuthManager() {
        initialize { image ->
            if (image != null) {
                Handler(Looper.getMainLooper()).post {
                    imageButtonAvatar.setImageBitmap(
                        image
                    )
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    imageButtonAvatar.setImageResource(
                        R.mipmap.avatar
                    )
                }
            }
        }

        if (!AuthManager.getIsVerified()) setNotificationDotVisible(View.VISIBLE)
    }

    private fun initializeNote() {
        val handler = object : OnNoteChangeHandler {
            override fun onChange(note: RecyclerViewItem) {
                Handler(Looper.getMainLooper()).post {
                    recyclerViewAdapter.noteChanged(note)
                }
            }

            override fun onAdd(note: RecyclerViewItem) {
                Handler(Looper.getMainLooper()).post {
                    recyclerViewAdapter.addToList(note)
                    if (recyclerViewAdapter.itemCount == 1) setRecyclerViewEmpty(false)
                }
            }

            override fun onRemove(note: RecyclerViewItem) {
                Handler(Looper.getMainLooper()).post {
                    recyclerViewAdapter.removeNote(note)
                    if (recyclerViewAdapter.itemCount == 0) setRecyclerViewEmpty(true)
                }
            }

            override suspend fun onRemovedFromServer(title: String): Boolean {
                return withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { continuation ->
                        val dialogView =
                            layoutInflater.inflate(R.layout.dialog_main_deleted_from_server, null)
                        val message = getString(R.string.note_removed_from_server, title)

                        val dialog = AlertDialog.Builder(this@MainActivity).setView(dialogView)
                            .setOnCancelListener { dialog ->
                                dialog.dismiss()
                                if (continuation.isActive) continuation.resume(false)
                            }.create()

                        dialogView.findViewById<TextView>(R.id.dialog_message).text =
                            Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)

                        dialogView.findViewById<Button>(R.id.dialog_positive).setOnClickListener {
                            dialog.dismiss()
                            if (continuation.isActive) continuation.resume(true)
                        }
                        dialogView.findViewById<Button>(R.id.dialog_negative).setOnClickListener {
                            dialog.dismiss()
                            if (continuation.isActive) continuation.resume(false)
                        }

                        // Handle coroutine cancellation (dismiss dialog if scope is canceled)
                        continuation.invokeOnCancellation { dialog.dismiss() }

                        dialog.show()
                    }
                }
            }

            override fun onUserChanged() {
                recyclerViewAdapter.setVisibleListAndRefreshView()
            }
        }
        Notes.initialize(handler)
    }

    private fun setRecyclerViewEmpty(isEmpty: Boolean) {
        swipeRefresh.post {
            if (isEmpty) {
                swipeRefresh.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                swipeRefresh.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        }
    }

    private fun checkTime() {
        val now = System.currentTimeMillis()

        //Do not sync at the first time when app opened since it maybe before the auth check.
        if (lastActiveTime != -1L && now - lastActiveTime >= 5 * 60 * 1000) {
            lifecycleScope.launch {
                Notes.getNotesFromApi()
                Message.show("Auto synced.")
            }
        }

        lastActiveTime = now
    }

    private fun setNotificationDotVisible(visible: Int) {
        viewNotificationDot.visibility = visible
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showVerifyEmailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_main_verify_email, null)

        val digits = arrayOf(
            dialogView.findViewById(R.id.etDigit1),
            dialogView.findViewById(R.id.etDigit2),
            dialogView.findViewById(R.id.etDigit3),
            dialogView.findViewById(R.id.etDigit4),
            dialogView.findViewById(R.id.etDigit5),
            dialogView.findViewById<VerifyEmailEditText>(R.id.etDigit6)
        )

        fun handleOnPaste(text: String) {
            if (text.length == 6 && text.all { it.isDigit() }) {
                text.forEachIndexed { index, c ->
                    digits[index].setText(c.toString())
                }
                digits.last().requestFocus()
            }
        }

        // Auto move cursor to next digit
        digits.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < digits.size - 1) {
                        digits[index + 1].requestFocus()
                    }
                }
            })
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && editText.text!!.isNotEmpty()) {
                    editText.selectAll()
                }
            }
            editText.setOnTouchListener { _, _ ->
                if (!editText.hasFocus()) {
                    editText.requestFocus()
                    editText.post { editText.selectAll() }
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, 0)

                true
            }
            editText.setOnPasteHandler { code ->
                Handler(Looper.getMainLooper()).post {
                    handleOnPaste(code)
                }
            }
            editText.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (index > 0) {
                        editText.setText("")
                        digits[index - 1].requestFocus()
                    }
                }
                return@setOnKeyListener false
            }
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val btnVerify = dialogView.findViewById<Button>(R.id.btnVerify)
        val btnResend = dialogView.findViewById<Button>(R.id.btnResend)
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)

        fun startResendTimer(endTime: Long) {
            val remaining = endTime - System.currentTimeMillis()
            if (remaining <= 0) {
                btnResend.isEnabled = true
                btnResend.text = getString(R.string.resend)
                return
            }

            btnResend.isEnabled = false
            object : CountDownTimer(remaining, 1_000) {
                override fun onTick(millisUntilFinished: Long) {
                    val t = millisUntilFinished / 1000
                    btnResend.text = getString(R.string.resend_time, t)
                }

                override fun onFinish() {
                    btnResend.isEnabled = true
                    btnResend.text = getString(R.string.resend)
                }
            }.start()
        }

        btnVerify.setOnClickListener {
            lifecycleScope.launch {
                val code = digits.joinToString("") { it.text.toString() }
                if (code.length != 6) {
                    Message.show(getInstance().getContext().getString(R.string.enter_code))
                } else {
                    if (AuthManager.verifyEmail(code)) {
                        setNotificationDotVisible(View.GONE)
                        dialog.dismiss()
                    }
                }
            }
        }
        btnResend.setOnClickListener {
            lifecycleScope.launch {
                if (AuthManager.resendVerifyEmail()) {
                    btnResend.isEnabled = false
                    resendAvailableAt = System.currentTimeMillis() + 60_000L

                    // Start the countdown
                    startResendTimer(resendAvailableAt)
                }
            }
        }
        btnLogout.setOnClickListener {
            btnVerify.isEnabled = false
            btnResend.isEnabled = false
            btnLogout.isEnabled = false
            lifecycleScope.launch {
                if (emailLogout()) dialog.dismiss()
                btnVerify.isEnabled = true
                btnResend.isEnabled = true
                btnLogout.isEnabled = true
                setNotificationDotVisible(View.GONE)
            }
        }

        dialog.show()

        startResendTimer(resendAvailableAt)
    }

    fun showInputPassword() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_main_password, null)
        val inputPassword = dialogView.findViewById<EditText>(R.id.inputPassword)

        val dialog =
            android.app.AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        inputPassword.post {
            inputPassword.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputPassword, 0)
        }
        inputPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                dialogView.findViewById<Button>(R.id.buttonPositive).performClick()
                true
            } else {
                false
            }
        }
        dialogView.findViewById<Button>(R.id.buttonPositive).setOnClickListener {
            try {
                val password = inputPassword.text.toString()
                if (password.isEmpty()) {
                    Message.show(getString(R.string.password_empty))
                } else {
                    val content = Crypto.encrypt(password, "")
                    val noteId = Notes.create(
                        "", content, "", 0, 0, false, AuthManager.getUserId(), true
                    )
                    if (noteId != -1) openEditor(noteId)
                    dialog.dismiss()
                }
            } catch (_: Exception) {
                Message.show(getString(R.string.incorrect_password))
            }
        }
        dialogView.findViewById<Button>(R.id.buttonNegative).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMenu() {
        val list: MutableList<ListViewItem> = ArrayList()
        for (item in menuItemList) {
            if (item.isShowing()) list.add(item)
        }
        val listViewAdapter = ListViewAdapter(this, list)
        val params = menuContainer.layoutParams

        var totalHeight = 0
        for (i in 0 until listViewAdapter.count) {
            val listItem = listViewAdapter.getView(i, null, listViewSetting)
            listItem.measure(
                View.MeasureSpec.makeMeasureSpec(listViewSetting.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            totalHeight += listItem.measuredHeight
        }
        totalHeight += listViewSetting.dividerHeight * (listViewAdapter.count - 1)

        if (totalHeight <= 247 * this@MainActivity.resources.displayMetrics.density) {
            params.height = 0
        } else {
            params.height = (247 * this@MainActivity.resources.displayMetrics.density).toInt()
        }
        params.width = (175 * this@MainActivity.resources.displayMetrics.density).toInt()
        menuContainer.layoutParams = params

        listViewSetting.adapter = listViewAdapter
        listViewSetting.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            list.getOrNull(position)?.onClick()
            listViewAdapter.notifyDataSetChanged()
            hideMenu()
        }

        menuContainer.visibility = View.VISIBLE
    }

    private fun hideMenu() {
        menuContainer.visibility = View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initializeSettingLauncher() {
        settingLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val returnValue = result.data?.getIntExtra("result", 0) ?: 0

                    when (returnValue) {
                        1 -> {
                            recyclerViewAdapter.notifyDataSetChanged()
                        }

                        2 -> {
                        }

                        3 -> {
                        }
                    }
                }
            }
    }
}