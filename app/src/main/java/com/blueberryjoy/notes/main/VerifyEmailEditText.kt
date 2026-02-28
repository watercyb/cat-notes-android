package com.blueberryjoy.notes.main

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

class VerifyEmailEditText(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs) {

    private lateinit var onPasteHandler: OnPasteHandler

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(outAttrs)
        return PasteInputConnection(ic, this)
    }

    fun interface OnPasteHandler {
        fun onPaste(code: String)
    }

    fun setOnPasteHandler(onPasteHandler: OnPasteHandler) {
        this.onPasteHandler = onPasteHandler
    }

    private class PasteInputConnection(
        target: InputConnection?,
        val editText: VerifyEmailEditText
    ) : InputConnectionWrapper(target, true) {

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {

            if (text != null && text.length > 1) {
                editText.onPasteHandler.onPaste(text.toString())
                return true
            }

            return super.commitText(text, newCursorPosition)
        }
    }
}