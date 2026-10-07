package com.blueberryjoy.notes.utils

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.R
import java.io.File
import java.io.RandomAccessFile

object FontManager {
    private const val PREF_NAME = "APP_DATA"
    private const val KEY_FONT_NAME = "custom_font_name"
    private const val KEY_FONT_SIZE_FACTOR = "font_size_factor"
    private const val FONT_DIR = "fonts"

    private fun context(): Context {
        return App.getInstance().getContext()
    }

    private fun preferences() =
        context().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun importFont(uri: Uri): Boolean {
        val context = context()
        var tempFile: File? = null

        return try {
            val fontDir = File(context.filesDir, FONT_DIR)
            if (!fontDir.exists() && !fontDir.mkdirs()) return false
            val extension = getFontExtension(uri)

            tempFile = File.createTempFile("font_import_", ".tmp", fontDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                tempFile.delete()
                return false
            }

            val fontName = getNameFromFont(tempFile)

            val destination = getUniqueFontFile(fontDir, fontName, extension)

            if (!tempFile.renameTo(destination)) {
                tempFile.delete()
                return false
            }
            true
        } catch (_: Exception) {
            tempFile?.delete()
            false
        }
    }

    fun setFontSizeFactor(progress: Int) {
        var percent = progressToFactor(progress)
        if (percent < 50) percent = 50
        if (percent > 150) percent = 150
        preferences().edit { putInt(KEY_FONT_SIZE_FACTOR, percent) }
    }

    fun getFontSizeFactor(): Int {
        return preferences().getInt(KEY_FONT_SIZE_FACTOR, 100)
    }

    fun getFontName(): String {
        var name = preferences().getString(KEY_FONT_NAME, "")
        if (name == null) name = ""
        return name
    }

    fun isCustomFontEnabled(): Boolean {
        return getFontName().isNotEmpty()
    }

    private fun getFontFile(): File {
        return File(File(context().filesDir, FONT_DIR), getFontName())
    }

    private fun getCustomTypeface(): Typeface? {
        if (!isCustomFontEnabled()) return null

        val fontFile = getFontFile()

        if (!fontFile.exists()) {
            preferences().edit { putString(KEY_FONT_NAME, "") }
            return null
        }

        return try {
            Typeface.createFromFile(fontFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun apply(view: View) {
        if (view !is TextView && view !is EditText) return

        val originalSize = view.tag.toString().toFloat()

        if (isCustomFontEnabled()) {
            val typeface = getCustomTypeface()
            if (typeface != null) view.typeface = typeface

            val factor = getFontSizeFactor()
            view.textSize = originalSize * factor / 100f
        } else {
            getDefaultTypeface()?.let { view.typeface = it }
            view.textSize = originalSize
        }
    }

    fun apply(view: View, progress: Int) {
        if (view !is TextView && view !is EditText) return
        view.textSize = view.tag.toString().toFloat() * progressToFactor(progress) / 100f
    }

    private fun getDefaultTypeface(): Typeface? {
        return ResourcesCompat.getFont(context(), R.font.text)
    }

    private fun getNameFromFont(file: File): String {
        RandomAccessFile(file, "r").use { raf ->

            // Offset table
            raf.seek(4)
            val numTables = raf.readUnsignedShort()

            // Table records start at byte 12
            raf.seek(12)

            // Find the "name" table
            var nameTableOffset = -1L

            repeat(numTables) {
                val tag = ByteArray(4)
                raf.readFully(tag)

                raf.skipBytes(4) // checksum

                val offset = raf.readInt().toLong() and 0xFFFFFFFFL
                raf.skipBytes(4) // length

                if (String(tag, Charsets.US_ASCII) == "name") {
                    nameTableOffset = offset
                }
            }

            if (nameTableOffset < 0) {
                return file.nameWithoutExtension
            }

            // name table
            raf.seek(nameTableOffset)

            raf.readUnsignedShort() // format
            val count = raf.readUnsignedShort()
            val stringOffset = raf.readUnsignedShort()

            var bestName: String? = null

            repeat(count) {
                val platformId = raf.readUnsignedShort()
                raf.readUnsignedShort() // encodingId
                raf.readUnsignedShort() // languageId
                val nameId = raf.readUnsignedShort()
                val length = raf.readUnsignedShort()
                val offset = raf.readUnsignedShort()

                // Name ID 1 = Font Family name
                if (nameId != 1) {
                    return@repeat
                }

                val currentPosition = raf.filePointer

                raf.seek(
                    nameTableOffset +
                            stringOffset +
                            offset
                )

                val bytes = ByteArray(length)
                raf.readFully(bytes)

                val name = when (platformId) {
                    // Unicode / Windows
                    0, 3 -> String(bytes, Charsets.UTF_16BE)

                    // Macintosh
                    1 -> String(bytes, Charsets.ISO_8859_1)

                    else -> null
                }

                raf.seek(currentPosition)

                if (!name.isNullOrBlank()) {
                    // Prefer Windows/Unicode
                    if (platformId == 3 || platformId == 0) {
                        return name
                    }

                    if (bestName == null) {
                        bestName = name
                    }
                }
            }

            return bestName ?: file.nameWithoutExtension
        }
    }

    private fun getUniqueFontFile(directory: File, fontName: String, extension: String): File {
        var file = File(directory, "$fontName.$extension")
        var number = 1
        while (file.exists()) {
            file = File(directory, "$fontName ($number).$extension")
            number++
        }
        return file
    }

    private fun getFontExtension(uri: Uri): String {
        val name = uri.lastPathSegment ?: ""
        return when {
            name.endsWith(".otf", true) -> "otf"
            else -> "ttf"
        }
    }

    fun getFonts(): MutableList<File> {
        val fontDir = File(context().filesDir, FONT_DIR)

        if (!fontDir.exists() || !fontDir.isDirectory) {
            return mutableListOf()
        }

        val fonts = fontDir.listFiles { file ->
            file.isFile &&
                    (file.extension.equals("ttf", ignoreCase = true) ||
                            file.extension.equals("otf", ignoreCase = true))
        }?.sortedBy { it.name.lowercase() }?.toMutableList()
            ?: mutableListOf()
        fonts.add(0, File(""))
        return fonts
    }

    fun setFontName(font: File) {
        if (font.name.isNotEmpty()) {
            preferences().edit { putString(KEY_FONT_NAME, font.name) }
        } else {
            preferences().edit { putString(KEY_FONT_NAME, "") }
        }
    }

    fun progressToFactor(progress: Int): Int {
        return progress + 50
    }

    fun factorToProgress(progress: Int): Int {
        return progress - 50
    }
}