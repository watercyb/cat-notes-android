package com.blueberryjoy.notes.editor

import android.media.Image
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import com.blueberryjoy.notes.R
import java.util.ArrayDeque

class UndoRedoHelper(
    private val editText: EditText,
    private val undo: ImageButton,
    private val redo: ImageButton
) {
    private var isUndoOrRedo = false
    private val undoStack = ArrayDeque<EditHistory>()
    private val redoStack = ArrayDeque<EditHistory>()

    private data class EditHistory(
        val start: Int,
        val beforeText: CharSequence,
        val afterText: CharSequence
    )

    init {
        editText.addTextChangedListener(object : TextWatcher {
            private var beforeChange: CharSequence = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                if (!isUndoOrRedo) {
                    beforeChange = s.subSequence(start, start + count)
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!isUndoOrRedo) {
                    val afterChange = s.subSequence(start, start + count)
                    undoStack.push(EditHistory(start, beforeChange, afterChange))
                    redoStack.clear()
                }
                checkButton()
            }

            override fun afterTextChanged(s: Editable) {}
        })
        checkButton()
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun undo() {
        if (!canUndo) return
        val history = undoStack.pop()
        redoStack.push(history)

        isUndoOrRedo = true
        val editable = editText.text
        val end = history.start + history.afterText.length
        editable.replace(history.start, end, history.beforeText)
        Selection.setSelection(editable, history.start + history.beforeText.length)
        isUndoOrRedo = false
    }

    fun redo() {
        if (!canRedo) return
        val history = redoStack.pop()
        undoStack.push(history)

        isUndoOrRedo = true
        val editable = editText.text
        val end = history.start + history.beforeText.length
        editable.replace(history.start, end, history.afterText)
        Selection.setSelection(editable, history.start + history.afterText.length)
        isUndoOrRedo = false
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    fun checkButton() {
        if (canUndo) {
            undo.setImageResource(R.mipmap.ic_undo)
            undo.isEnabled = true
        } else {
            undo.setImageResource(R.mipmap.ic_undo_grey)
            undo.isEnabled = false
        }
        if (canRedo) {
            redo.setImageResource(R.mipmap.ic_redo)
            redo.isEnabled = true
        } else {
            redo.setImageResource(R.mipmap.ic_redo_grey)
            redo.isEnabled = false
        }
    }
}