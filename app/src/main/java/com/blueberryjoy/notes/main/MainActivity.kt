package com.blueberryjoy.notes.main

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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
import com.blueberryjoy.notes.R
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
import com.blueberryjoy.notes.utils.Message
import kotlinx.coroutines.launch
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private var imageButtonSetting: ImageButton? = null
    private var listViewSetting: ListView? = null
    private var recyclerView: RecyclerView? = null
    private var imageButtonAvatar: ImageButton? = null

    private var recyclerViewAdapter: RecyclerViewAdapter? = null
    private lateinit var editorLauncher: ActivityResultLauncher<Intent>

    private var lastActiveTime: Long = -1L

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

        //window.navigationBarColor = getColor(R.color.background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        initializeViews()
        initializeViewModel()
        initializeNote()
        initializeListView()
        initializeRecyclerView()
        //Should after the initialization of recyclerView to avoid changing list during that process.
        initializeAuthManager()
        initializeEditorLauncher()

        checkTime()
    }

    public override fun onResume() {
        super.onResume()
        checkTime()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.rawX.toInt()
        val y = ev.rawY.toInt()
        val rect = Rect()
        listViewSetting!!.getGlobalVisibleRect(rect)
        if (!rect.contains(x, y)) {
            imageButtonSetting!!.getGlobalVisibleRect(rect)
            if (!rect.contains(x, y)) listViewSetting!!.visibility = View.GONE
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initializeListView() {
        listViewSetting!!.translationZ = 10f
        val list: MutableList<ListViewItem?> = ArrayList()

        list.add(ListViewItem("NEW NOTE") {
            val noteId = Notes.create("", "", null, 0)
            if (noteId != -1) openEditor(noteId)
        })
        list.add(ListViewItem("LOGIN") {
            val intent = Intent(this@MainActivity, Login::class.java)
            startActivity(intent)
        })
        list.add(ListViewItem("REFRESH") {
            lifecycleScope.launch {
                Notes.getNotesFromApi()
            }
        })
        list.add(ListViewItem("G-LOGOUT") {})
        list.add(ListViewItem("TEST") {})
        list.add(ListViewItem("SETTING") {})

        val listViewAdapter = ListViewAdapter(this, list)
        val params = listViewSetting!!.layoutParams
        params.height =
            ((min(listViewAdapter.count * 41, 250) + 15) * this@MainActivity.getResources()
                .displayMetrics.density).toInt()
        params.width = (175 * this@MainActivity.getResources().displayMetrics.density).toInt()
        listViewSetting!!.layoutParams = params

        listViewSetting!!.adapter = listViewAdapter
        listViewSetting!!.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            list.getOrNull(position)?.onClick()
            listViewSetting?.visibility = View.GONE
        }
    }

    private fun initializeRecyclerView() {
        val list = Notes.list

        val spanCount = getResources().getInteger(R.integer.grid_span)
        recyclerView!!.setLayoutManager(
            StaggeredGridLayoutManager(
                spanCount, StaggeredGridLayoutManager.VERTICAL
            )
        )
        recyclerViewAdapter = RecyclerViewAdapter(this, list)
        recyclerViewAdapter!!.setClickListener { _: View?, position: Int ->
            openEditor(list[position].id)
        }
        recyclerView!!.setAdapter(recyclerViewAdapter)

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
                Notes.swap(from, to)
                recyclerViewAdapter!!.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        }

        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)
    }

    private fun openEditor(id: Int) {
        val intent = Intent(this@MainActivity, Editor::class.java)
        intent.putExtra("ID", id)
        editorLauncher.launch(intent)
    }

    private fun initializeEditorLauncher() {
        editorLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null && recyclerViewAdapter != null) {
                val position =
                    result.data!!.getIntExtra("POSITION", recyclerViewAdapter!!.itemCount)
                when {
                    position < 0 -> recyclerViewAdapter!!.notifyItemRemoved(-position - 1)
                    position < recyclerViewAdapter!!.itemCount -> recyclerViewAdapter!!.notifyItemChanged(
                        position
                    )
                }
            }
        }
    }

    private fun initializeViews() {
        imageButtonSetting = findViewById(R.id.imageButtonSetting)
        listViewSetting = findViewById(R.id.listViewSetting)
        recyclerView = findViewById(R.id.recyclerView)
        imageButtonAvatar = findViewById(R.id.imageButtonAvatar)
        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.swipeRefresh)

        imageButtonSetting!!.setOnClickListener { v: View? ->
            v!!.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
                .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }.start()
            if (listViewSetting!!.isVisible) {
                listViewSetting!!.visibility = View.GONE
            } else {
                listViewSetting!!.visibility = View.VISIBLE
            }
        }
        imageButtonAvatar!!.setOnClickListener {
            if (getType() == AuthType.EMAIL) {
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

        swipeRefresh.setOnRefreshListener {
            // Trigger your refresh logic
            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefresh.isRefreshing = false
            }, 5000)
            lifecycleScope.launch {
                Notes.getNotesFromApi()
                swipeRefresh.isRefreshing = false
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
                    imageButtonAvatar!!.setImageBitmap(
                        image
                    )
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    imageButtonAvatar!!.setImageResource(
                        R.mipmap.avatar
                    )
                }
            }
        }
    }

    private fun initializeNote() {
        val handler = object : OnNoteChangeHandler {
            override fun onChange(idx: Int) {
                Handler(Looper.getMainLooper()).post {
                    recyclerViewAdapter?.notifyItemChanged(idx)
                }
            }

            override fun onAdd(idx: Int) {
                Handler(Looper.getMainLooper()).post {
                    recyclerViewAdapter?.notifyItemInserted(idx)
                }
            }

            override fun onRemove(idx: Int) {
                Handler(Looper.getMainLooper()).post {
                    recyclerViewAdapter?.notifyItemRemoved(idx)
                }
            }
        }
        Notes.initialize(handler)
    }

    private fun checkTime() {
        val now = System.currentTimeMillis()

        if (lastActiveTime != -1L && now - lastActiveTime >= 5 * 60 * 1000) {
            Message.show("sync")
            lifecycleScope.launch {
                Notes.getNotesFromApi()
            }
        }

        lastActiveTime = now
    }
}