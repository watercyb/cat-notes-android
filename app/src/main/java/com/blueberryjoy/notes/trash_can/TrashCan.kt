package com.blueberryjoy.notes.trash_can

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
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

    private var recyclerView: RecyclerView? = null
    private var recyclerViewAdapter: RemovedRecyclerViewAdapter? = null

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

        initializeRecyclerView()
    }

    private fun initializeRecyclerView() {
        val list = Notes.removedList

        recyclerView!!.setLayoutManager(LinearLayoutManager(this@TrashCan))
        recyclerViewAdapter = RemovedRecyclerViewAdapter(this@TrashCan, list)
        recyclerViewAdapter!!.setClickListener { _: View?, position: Int ->
            showRemoveOrRestoreDialog(list[position])
        }
        recyclerView!!.setAdapter(recyclerViewAdapter)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showRemoveOrRestoreDialog(note: RecyclerViewItem) {
        val title = if (note.title.trim().isEmpty()) "<No Title>" else note.title
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Do you want to permanently delete this note or restore it?")
            .setNegativeButton("Restore") { dialogInterface, _ ->
                Notes.restoreFromTrashCan(note)
                recyclerViewAdapter?.getVisibleList()
                recyclerViewAdapter?.notifyDataSetChanged()
                dialogInterface.dismiss()
                //onRestore()
            }
            .setPositiveButton("Delete") { dialogInterface, _ ->
                Notes.removeFromTrashCan(note)
                recyclerViewAdapter?.getVisibleList()
                recyclerViewAdapter?.notifyDataSetChanged()
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()

        // Make Delete button red
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.RED)
    }
}