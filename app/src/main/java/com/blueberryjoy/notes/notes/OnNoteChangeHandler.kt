package com.blueberryjoy.notes.notes

import com.blueberryjoy.notes.utils.RecyclerViewItem

interface OnNoteChangeHandler {
    fun onChange(note: RecyclerViewItem)
    fun onAdd(note: RecyclerViewItem)
    fun onRemove(note: RecyclerViewItem)

    //Ask user if remove note when it is removed from server.
    suspend fun onRemovedFromServer(title: String): Boolean

    fun onUserChanged()
}