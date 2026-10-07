package com.blueberryjoy.notes.notes

import com.blueberryjoy.notes.utils.RecyclerViewItem
import com.blueberryjoy.notes.utils.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.time.Duration.Companion.milliseconds

object SyncQueue {
    val queue = LinkedList<Int>()
    val HM = HashMap<Int, String>()
    const val DELAY: Long = 60000
    var isRunning = false
    var isUpdatingNotesFromApi = false
    var queueId = 0

    suspend fun startQueue(id: Int) {
        if (isRunning || isUpdatingNotesFromApi) return
        isRunning = true
        while (!queue.isEmpty() && id == queueId) {
            val id = queue.peekFirst()
            if (id == null) {
                queue.pollFirst()
                continue
            }
            val mongoId = HM[id]
            try {
                var success: Boolean
                if (id < 0) {
                    success = mongoId == null || Notes.getNoteFromApi(mongoId)
                } else {
                    val idx = Notes.getNoteById(id)
                    if (idx == -1) {
                        success = mongoId == null || Notes.deleteNoteFromApi(mongoId)
                    } else {
                        val note: RecyclerViewItem = Notes.activeNotes[idx]
                        if (note.mongoId == null || note.mongoId.isEmpty()) {
                            Message.logW("Run createNote: $note " + note.title)
                            success = Notes.createNoteOnApi(note)
                        } else if (note.mongoId.equals("REMOVED FROM SERVER")) {
                            Message.logW("Should not be called: $note " + note.title)
                            success = Notes.createNoteOnApi(note)
                        } else {
                            Message.logW("Run updateNote: $note " + note.title)
                            success = Notes.updateNoteToApi(note)
                        }
                    }
                }
                if (!success) {
                    delay(DELAY.milliseconds)
                } else {
                    if (queue.isNotEmpty())
                        queue.pollFirst()
                    HM.remove(id)
                }
            } catch (_: Exception) {
                delay(DELAY.milliseconds)
            }
        }
        isRunning = false
    }

    fun add(note: RecyclerViewItem) {
        if (!HM.containsKey(note.id)) {
            Message.logW("Add to queue: title ${note.title}, mongoId ${note.mongoId}")
            HM[note.id] = note.mongoId
            queue.offerLast(note.id)
            val id = ++queueId
            CoroutineScope(Dispatchers.Main).launch {
                startQueue(id)
            }
        }
    }

    fun add(mongoId: String) {
        var id = -1
        while (id >= -1000) {
            if (!HM.containsKey(id)) break
            id--
        }
        Message.logW("Add to queue: mongoId $mongoId")
        HM[id] = mongoId
        queue.offerLast(id)
        val nextId = ++queueId
        CoroutineScope(Dispatchers.Main).launch {
            startQueue(nextId)
        }
    }

    fun setIsUpdatingNotesFromApi(isUpdatingNotesFromApi: Boolean) {
        if (isUpdatingNotesFromApi) {
            SyncQueue.isUpdatingNotesFromApi = true
            clear()
        } else {
            SyncQueue.isUpdatingNotesFromApi = false
            val id = ++queueId
            CoroutineScope(Dispatchers.Main).launch {
                startQueue(id)
            }
        }
    }

    @Suppress("unused")
    fun getSize(): Int {
        return queue.size
    }

    fun clear() {
        queue.clear()
        HM.clear()
        isRunning = false
    }

    fun showQueue() {
        Message.debug("${queue.size}")
        for (id in queue) {
            Message.logW("Id: $id, mongoId: ${HM[id]}")
        }
    }
}