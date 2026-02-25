package com.blueberryjoy.notes.notes

interface OnNoteChangeHandler {
    fun onChange(idx: Int)
    fun onAdd(idx: Int)
    fun onRemove(idx: Int)
}