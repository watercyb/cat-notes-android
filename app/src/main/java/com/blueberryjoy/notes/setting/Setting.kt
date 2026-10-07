package com.blueberryjoy.notes.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.utils.FontManager
import com.blueberryjoy.notes.utils.Message

class Setting : AppCompatActivity() {
    private var fontRecyclerViewAdapter: FontRecyclerViewAdapter? = null
    private lateinit var fontName: TextView
    private lateinit var fontSize: LinearLayout
    private lateinit var fontSizePercentage: TextView
    private lateinit var fontSizeSeekbar: SeekBar
    private var result = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.change_font).setOnClickListener {
            showFontDialog()
        }

        fontName = findViewById(R.id.font_name)
        fontSize = findViewById(R.id.font_size)
        fontSizePercentage = findViewById(R.id.font_size_percent)
        fontSizeSeekbar = findViewById(R.id.font_size_seekbar)

        fontSizeSeekbar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    FontManager.apply(fontName, p1)
                    updateFontSizePercentage(p1)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    result = result or 1
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    FontManager.setFontSizeFactor(p0!!.progress)
                }
            }

        )

        updateFontViews()
        updateFontSizePercentage()
        updateFontSizeSeekbar()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent()
                intent.putExtra("result", result)
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    private val fontPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                val success = FontManager.importFont(uri)

                if (success) {
                    fontRecyclerViewAdapter?.updateFonts(FontManager.getFonts())
                    Message.show(getString(R.string.font_imported))
                } else {
                    Message.show(getString(R.string.unable_import_font))
                }
            }
        }

    private fun showFontDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_setting_font, null)
        val dialog = android.app.AlertDialog.Builder(this).setView(dialogView).create()

        val fonts = FontManager.getFonts()
        fontRecyclerViewAdapter = FontRecyclerViewAdapter(fonts) { font ->
            FontManager.setFontName(font)
            updateFontViews()
            result = result or 1
            dialog.dismiss()
        }

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.font_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fontRecyclerViewAdapter

        dialogView.findViewById<Button>(R.id.cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.import_font).setOnClickListener {
            fontPicker.launch(
                arrayOf(
                    "font/ttf",
                    "font/otf",
                    "application/x-font-ttf",
                    "application/x-font-opentype",
                    "application/octet-stream"
                )
            )
        }

        dialog.show()
    }

    private fun updateFontViews() {
        val name = FontManager.getFontName()
        if (name.isEmpty()) {
            fontName.text = getString(R.string.default_font)
        } else {
            fontName.text = name.split(".").first()
        }
        FontManager.apply(fontName)
        if (FontManager.isCustomFontEnabled()) {
            fontSize.visibility = View.VISIBLE
        } else {
            fontSize.visibility = View.GONE
        }
    }


    private fun updateFontSizePercentage() {
        fontSizePercentage.text = buildString {
            append(FontManager.getFontSizeFactor())
            append("%")
        }
    }

    private fun updateFontSizePercentage(progress: Int) {
        fontSizePercentage.text = buildString {
            append(FontManager.progressToFactor(progress))
            append("%")
        }
    }

    private fun updateFontSizeSeekbar() {
        fontSizeSeekbar.progress = FontManager.factorToProgress(FontManager.getFontSizeFactor())
    }
}