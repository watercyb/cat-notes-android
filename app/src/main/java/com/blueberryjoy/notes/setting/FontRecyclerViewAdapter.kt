package com.blueberryjoy.notes.setting

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.utils.FontManager
import java.io.File

class FontRecyclerViewAdapter(
    private val fonts: MutableList<File>,
    private val onFontSelected: (File) -> Unit
) : RecyclerView.Adapter<FontRecyclerViewAdapter.FontViewHolder>() {

    class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.font_name)
        val delete: ImageButton = itemView.findViewById(R.id.delete_font)
        val linearLayout: LinearLayout = itemView.findViewById(R.id.linearLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_setting_item, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val font = fonts[position]
        if (font.path.isEmpty() && position == 0) {
            holder.name.text = App.getInstance().getContext().getString(R.string.default_font)
            holder.name.typeface =
                ResourcesCompat.getFont(App.getInstance().getContext(), R.font.text)
            holder.delete.visibility = View.GONE
        } else {
            holder.name.text = font.name
            try {
                holder.name.typeface = Typeface.createFromFile(font)
            } catch (_: Exception) {
                holder.name.typeface = Typeface.DEFAULT
            }
            holder.delete.visibility=View.VISIBLE
            holder.delete.setOnClickListener {
                deleteFont(font)
            }
        }
        holder.itemView.setOnClickListener {
            onFontSelected(font)
        }
        if (font.name == FontManager.getFontName()) {
            holder.linearLayout.setBackgroundResource(R.color.current_font_background)
        } else {
            holder.linearLayout.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int {
        return fonts.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFonts(newFonts: List<File>) {
        fonts.clear()
        fonts.addAll(newFonts)
        notifyDataSetChanged()
    }

    private fun deleteFont(font: File) {
        val position = fonts.indexOf(font)
        if (position < 0 || !font.delete()) return
        fonts.removeAt(position)
        notifyItemRemoved(position)
        if (font.name == FontManager.getFontName())
            onFontSelected(fonts[position - 1])
    }
}