package com.blueberryjoy.notes.trash_can

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.notes.Notes
import com.blueberryjoy.notes.utils.RecyclerViewItem

class TrashCan : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: RemovedRecyclerViewAdapter
    private lateinit var emptyView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trash_can)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        } else {
            @Suppress("DEPRECATION")
            window.navigationBarColor = getColor(R.color.background)
        }

        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)

        initializeRecyclerView()
    }

    private fun initializeRecyclerView() {
        val list = Notes.removedList

        recyclerView.setLayoutManager(LinearLayoutManager(this@TrashCan))
        recyclerViewAdapter = RemovedRecyclerViewAdapter(this@TrashCan, list)
        recyclerViewAdapter.setClickListener { _: View?, position: Int ->
            showRemoveOrRestoreDialog(recyclerViewAdapter.visibleList[position])
        }
        recyclerView.setAdapter(recyclerViewAdapter)

        if (recyclerViewAdapter.itemCount == 0) setRecyclerViewEmpty()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showRemoveOrRestoreDialog(note: RecyclerViewItem) {
        val title = if (note.title.trim().isEmpty()) getString(R.string.no_title) else note.title
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(getString(R.string.delete_or_restore))
            .setNegativeButton(getString(R.string.restore)) { dialogInterface, _ ->
                Notes.restoreFromTrashCan(note)
                recyclerViewAdapter.setVisibleList()
                recyclerViewAdapter.notifyDataSetChanged()
                dialogInterface.dismiss()

                if (recyclerViewAdapter.itemCount == 0) setRecyclerViewEmpty()
            }
            .setPositiveButton(getString(R.string.delete)) { dialogInterface, _ ->
                Notes.removeFromTrashCan(note)
                recyclerViewAdapter.setVisibleList()
                recyclerViewAdapter.notifyDataSetChanged()
                dialogInterface.dismiss()

                if (recyclerViewAdapter.itemCount == 0) setRecyclerViewEmpty()
            }
            .create()

        dialog.show()

        // Make Delete button red
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.RED)
    }

    private fun setRecyclerViewEmpty() {
        recyclerView.post {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        }
    }
}