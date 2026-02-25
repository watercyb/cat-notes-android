package com.blueberryjoy.notes.notes

import com.blueberryjoy.notes.main.RecyclerViewItem
import com.blueberryjoy.notes.utils.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.LinkedList

object SyncQueue {
    val queue = LinkedList<Int>()
    val HM = HashMap<Int, String>()
    const val DELAY: Long = 60000
    var isRunning = false
    var isUpdatingNotesFromApi = false

    suspend fun startQueue() {
        if (isRunning && !isUpdatingNotesFromApi) return
        isRunning = true
        while (!queue.isEmpty()) {
            val id = queue.peekFirst()
            if (id == null) {
                queue.pollFirst()
                continue
            }
            val mongoId = HM[id]
            try {
                var success = false
                val idx = Notes.getNoteById(id)
                if (idx == -1) {
                    success = mongoId == null || Notes.deleteNodeFromApi(mongoId)
                } else {
                    val note: RecyclerViewItem = Notes.list[idx]
                    if (note.mongoId == null || note.mongoId.isEmpty()) {
                        Message.logW("Run createNote: $note " + note.title)
                        success = Notes.createNoteOnApi(note)
                    } else if (note.mongoId.equals("REMOVED FROM SERVER")) {
                        Message.logW("Run createNote on server removed: $note " + note.title)
                        success = Notes.createNoteOnApi(note)
                    } else {
                        Message.logW("Run updateNote: $note " + note.title)
                        success = Notes.updateNoteToApi(note)
                    }
                }
                if (!success) {
                    delay(DELAY)
                } else {
                    queue.pollFirst()
                    HM.remove(id)
                }
            } catch (_: Exception) {
                delay(DELAY)
            }
        }
        isRunning = false
    }

    fun add(note: RecyclerViewItem) {
        if (!HM.containsKey(note.id)) {
            Message.logW("Add to queue: " + note.title + " " + note.mongoId)
            HM[note.id] = note.mongoId
            queue.offerLast(note.id)
            CoroutineScope(Dispatchers.Main).launch {
                startQueue()
            }
        }
    }

    fun setIsUpdatingNotesFromApi(isUpdatingNotesFromApi: Boolean) {
        SyncQueue.isUpdatingNotesFromApi = isUpdatingNotesFromApi
        CoroutineScope(Dispatchers.Main).launch {
            startQueue()
        }
    }
}