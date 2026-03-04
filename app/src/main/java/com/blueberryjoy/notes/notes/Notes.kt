package com.blueberryjoy.notes.notes

import android.util.Log
import android.widget.Toast
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthManager.setLogout
import com.blueberryjoy.notes.auth.AuthType
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.retrofit.RetrofitInstance
import com.blueberryjoy.notes.retrofit.data_class.ApiNote
import com.blueberryjoy.notes.retrofit.data_class.ApiReq
import com.blueberryjoy.notes.sqlite.SQLite
import com.blueberryjoy.notes.utils.Message
import com.blueberryjoy.notes.utils.RecyclerViewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.function.ToIntFunction

object Notes {
    @JvmStatic
    var list: MutableList<RecyclerViewItem> = ArrayList()
        private set
    var removedList: MutableList<RecyclerViewItem> = ArrayList()
    private var onNoteChangeHandler: OnNoteChangeHandler? = null

    fun initialize(onNoteChangeHandler: OnNoteChangeHandler) {
        val notes = SQLite.initialize()
        list = ArrayList()
        for (note in notes) {
            if (note.removeTime == 0L) {
                list.add(RecyclerViewItem(note))
                Message.logW("${list[list.size - 1]}")
            } else {
                val recNote = RecyclerViewItem(note)
                if (recNote.removeTime < System.currentTimeMillis() && recNote.isRemovedFromServer) {
                    SQLite.delete(recNote)
                } else {
                    removedList.add(recNote)
                }
                Message.logW("${removedList[removedList.size - 1]}")
            }
        }
        list.sortWith(Comparator.comparingInt(ToIntFunction { a: RecyclerViewItem? -> a!!.idx }))
        for (i in list.indices) {
            list[i].idx = i * 10000
        }
        this.onNoteChangeHandler = onNoteChangeHandler
    }

    @JvmStatic
    fun getNoteById(id: Int): Int {
        for (i in list.indices) {
            if (list[i].id == id) return i
        }
        return -1
    }

    fun getNoteByMongoId(mongoId: String): Int {
        for (i in list.indices) {
            if (list[i].mongoId.equals(mongoId)) return i
        }
        return -1
    }

    fun create(
        title: String?,
        content: String?,
        mongoId: String?,
        version: Int,
        removeTime: Int,
        isRemovedFromServer: Boolean
    ): Int {
        try {
            val idx = (if (list.isEmpty()) 0 else list[list.size - 1].idx) + 10000
            val note = SQLite.create(
                title, content, mongoId, version, idx, removeTime, isRemovedFromServer
            )
            val item = RecyclerViewItem(note)
            list.add(item)
            return item.id
        } catch (e: Exception) {
            Toast.makeText(getInstance(), "error in Notes create: $e", Toast.LENGTH_LONG).show()
            Log.e("testLog", e.toString())
            return -1
        }
    }

    fun create(note: ApiNote): Int {
        val title = note.title
        val content = note.content
        val mongoId = note.mongoId
        val version = note.version
        try {
            val idx = (if (list.isEmpty()) 0 else list[list.size - 1].idx) + 10000
            val note = SQLite.create(title, content, mongoId, version, idx, 0, false)
            val item = RecyclerViewItem(note)
            list.add(item)
            return item.id
        } catch (e: Exception) {
            Toast.makeText(getInstance(), "error in Notes create: $e", Toast.LENGTH_LONG).show()
            Log.e("testLog", e.toString())
            return -1
        }
    }

    @JvmStatic
    fun removeFromList(note: RecyclerViewItem): Int {
        for (i in list.indices) {
            if (list[i].id == note.id) {
                list.removeAt(i)
                return i
            }
        }
        return -1
    }

    fun swap(i: Int, j: Int) {
        var i = i
        if (i > j) {
            while (i > j) {
                val a = list[i]
                val b = list[i - 1]
                val temp = a.idx
                a.idx = b.idx
                b.idx = temp
                list[i] = b
                list[i - 1] = a
                SQLite.saveIdx(a)
                SQLite.saveIdx(b)
                i--
            }
        } else {
            while (i < j) {
                val a = list[i]
                val b = list[i + 1]
                val temp = a.idx
                a.idx = b.idx
                b.idx = temp
                list[i] = b
                list[i + 1] = a
                SQLite.saveIdx(a)
                SQLite.saveIdx(b)
                i++
            }
        }
    }

    fun updateNote(notes: Array<ApiNote>?) {
        if (notes == null) return

        SyncQueue.setIsUpdatingNotesFromApi(true)
        val mongoIdToNotes: HashMap<String, RecyclerViewItem> = HashMap()

        for (note in list) {
            if (note.mongoId == null || note.mongoId.isEmpty()) {
                SyncQueue.add(note)
            } else {
                mongoIdToNotes[note.mongoId] = note
            }
        }

        for (note in removedList) {
            if (note.mongoId != null && !note.mongoId.isEmpty()) mongoIdToNotes[note.mongoId] = note
        }

        for (note in notes) {
            val recNote = mongoIdToNotes[note.mongoId]
            if (recNote != null) {
                compareNoteAndUpdateWithSameMongoId(recNote, note)
                mongoIdToNotes.remove(note.mongoId)
            } else {
                val newNote = SQLite.create(
                    note.title,
                    note.content,
                    note.mongoId,
                    note.version,
                    if (list.isEmpty()) 0 else list[list.size - 1].idx + 10000,
                    0,
                    false
                )
                list.add(RecyclerViewItem(newNote))
                onNoteChangeHandler?.onAdd(list.size - 1)
            }
        }

        for (note in mongoIdToNotes.values) {
            if (note.removeTime == 0L) {
                CoroutineScope(Dispatchers.Default).launch {
                    setNoteAsRemovedOnServer(note)
                }
            } else {
                note.mongoId = ""
                note.isRemovedFromServer = true
                SQLite.saveRemoval(note)
            }
        }
        SyncQueue.setIsUpdatingNotesFromApi(false)
    }

    fun compareNoteAndUpdateWithSameMongoId(note: RecyclerViewItem, apiNote: ApiNote) {
        if (note.removeTime != 0L) {
            SyncQueue.add(note)
        } else if (note.version > apiNote.version) {
            SyncQueue.add(note)
        } else if (note.version < apiNote.version) {
            note.title = apiNote.title
            note.content = apiNote.content
            note.version = apiNote.version
            SQLite.saveNote(note)
            onNoteChangeHandler?.onChange(getNoteById(note.id))
        } else if (!note.equals(apiNote)) {
            val newNote = SQLite.create(
                apiNote.title,
                apiNote.content,
                apiNote.mongoId,
                apiNote.version,
                if (list.isEmpty()) 0 else list[list.size - 1].idx + 10000,
                0,
                false
            )
            list.add(RecyclerViewItem(newNote))
            onNoteChangeHandler?.onAdd(list.size - 1)
            note.mongoId = ""
            SyncQueue.add(note)
            SQLite.saveMongoId(note)
        }
    }

    suspend fun createNoteOnApi(note: RecyclerViewItem): Boolean {
        if (AuthManager.getType() == AuthType.GOOGLE && !ClerkAuth.getToken()) return false

        val apiReq = ApiReq(title = note.title, content = note.content, version = note.version)
        try {
            val response = RetrofitInstance.api.createNote(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                Message.logW("Note created: $res")
                if (res?.success == true) {
                    note.mongoId = res.note?.mongoId
                    SQLite.saveMongoId(note)
                    return true
                }
                return false
            } else {
                Message.logE("$response")
                if (response.code() == 429) {
                    Message.showErrorFromThread("Too many requests, please try later.")
                } else if (response.code() == 401) {
                    Message.showErrorFromThread("Invalid credentials")
                    setLogout()
                } else if (response.code() == 500) {
                    Message.showErrorFromThread("Server internal error")
                } else {
                    Message.showErrorFromThread("Unexpected error")
                }
                return false
            }
        } catch (e: Exception) {
            Message.logW("Exception: $e")
            Message.showErrorFromThread("Cannot reach the server")
            return false
        }
    }

    suspend fun deleteNodeFromApi(mongoId: String): Boolean {
        if (AuthManager.getType() == AuthType.GOOGLE && !ClerkAuth.getToken()) return false

        try {
            val response = RetrofitInstance.api.deleteNote(mongoId)
            Message.logW("deleteNodeFromApi: $response")
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.logW("Note deleted: $res")
                    findAndSaveAsRemoved(mongoId)
                    return true
                }
                return false
            } else {
                Message.logE("$response")
                if (response.code() == 429) {
                    Message.showErrorFromThread("Too many requests, please try later.")
                } else if (response.code() == 401) {
                    Message.showErrorFromThread("Invalid credentials")
                    setLogout()
                } else if (response.code() == 500) {
                    Message.showErrorFromThread("Server internal error")
                } else {
                    Message.showErrorFromThread("Unexpected error")
                }
                return false
            }
        } catch (e: Exception) {
            Message.logW("Exception: $e")
            Message.showErrorFromThread("Cannot reach the server")
            return false
        }
    }

    suspend fun updateNoteToApi(note: RecyclerViewItem): Boolean {
        if (AuthManager.getType() == AuthType.GOOGLE && !ClerkAuth.getToken()) return false

        val apiReq = ApiReq(
            noteId = note.mongoId,
            title = note.title,
            content = note.content,
            version = note.version
        )
        try {
            val response = RetrofitInstance.api.updateNote(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.logW("Note updated: $res")
                    if (res.note != null) {
                        note.mongoId = res.note.mongoId
                        SQLite.saveMongoId(note)
                    } else {
                        Message.showErrorFromThread("Unexpected error")
                        return false
                    }
                    SQLite.saveMongoId(note)
                    return true
                } else if (res?.noteFromServer != null && res.newNote != null) {
                    note.mongoId = res.newNote.mongoId
                    SQLite.saveMongoId(note)
                    create(res.noteFromServer)
                    onNoteChangeHandler?.onAdd(list.size - 1)
                    Message.showFromThread("A newer version is found, both two versions are kept.")
                    return true
                } else {
                    Message.showErrorFromThread("Should not be called")
                }
                return false
            } else {
                Message.logE("$response")
                if (response.code() == 429) {
                    Message.showErrorFromThread("Too many requests, please try later.")
                } else if (response.code() == 401) {
                    Message.showErrorFromThread("Invalid credentials")
                    setLogout()
                } else if (response.code() == 500) {
                    Message.showErrorFromThread("Server internal error")
                } else {
                    Message.showErrorFromThread("Unexpected error")
                }
                return false
            }
        } catch (e: Exception) {
            Message.logW("Exception: $e")
            Message.showErrorFromThread("Cannot reach the server")
            return false
        }
    }

    suspend fun getNotesFromApi(): Boolean {
        if (AuthManager.getType() == AuthType.GOOGLE && !ClerkAuth.getToken()) return false

        try {
            val response = RetrofitInstance.api.getNotesIdAndVersion()
            if (response.isSuccessful) {
                val res = response.body()
                Message.logW(res.toString())
                if (res?.success == true) {
                    updateNoteWithoutTitleAndContents(res.notes)
                    return true
                } else {
                    Message.showErrorFromThread("Should not be called")
                }
                return false
            } else {
                Message.logE("$response")
                if (response.code() == 429) {
                    Message.showErrorFromThread("Too many requests, please try later.")
                } else if (response.code() == 401) {
                    Message.showErrorFromThread("Invalid credentials")
                    setLogout()
                } else if (response.code() == 500) {
                    Message.showErrorFromThread("Server internal error")
                } else {
                    Message.showErrorFromThread("Unexpected error")
                }
                return false
            }
        } catch (e: Exception) {
            Message.logW("Exception: $e")
            Message.showErrorFromThread("Cannot reach the server")
            return false
        }
    }

    fun updateNoteWithoutTitleAndContents(notes: Array<ApiNote>?) {
        if (notes == null) return

        SyncQueue.setIsUpdatingNotesFromApi(true)
        val mongoIdToNotes: HashMap<String, RecyclerViewItem> = HashMap()

        for (note in list) {
            if (note.mongoId == null || note.mongoId.isEmpty()) {
                SyncQueue.add(note)
            } else {
                mongoIdToNotes[note.mongoId] = note
            }
        }

        for (note in removedList) {
            if (note.mongoId != null && !note.mongoId.isEmpty()) mongoIdToNotes[note.mongoId] = note
        }

        for (note in notes) {
            val recNote = mongoIdToNotes[note.mongoId]
            if (recNote != null) {
                if (recNote.version != note.version || recNote.removeTime != 0L) SyncQueue.add(
                    recNote
                )
                mongoIdToNotes.remove(recNote.mongoId)
            } else {
                Message.logW(note.toString())
                SyncQueue.add(note.mongoId)
            }
        }

        for (note in mongoIdToNotes.values) {
            if (note.removeTime == 0L) {
                CoroutineScope(Dispatchers.Default).launch {
                    setNoteAsRemovedOnServer(note)
                }
            } else {
                note.mongoId = ""
                note.isRemovedFromServer = true
                SQLite.saveRemoval(note)
            }
        }
        SyncQueue.setIsUpdatingNotesFromApi(false)
    }

    suspend fun getNoteFromApi(mongoId: String): Boolean {
        if (AuthManager.getType() == AuthType.GOOGLE && !ClerkAuth.getToken()) return false

        try {
            val response = RetrofitInstance.api.getNote(mongoId)
            if (response.isSuccessful) {
                val res = response.body()
                Message.logW(res.toString())
                if (res?.success == true) {
                    //If note is not on the server, return true.
                    val apiNote = res.note ?: return true
                    val idx = getNoteByMongoId(apiNote.mongoId)
                    if (idx >= 0) {
                        val recNote = list[idx]
                        compareNoteAndUpdateWithSameMongoId(recNote, apiNote)
                    } else {
                        val newNote = SQLite.create(
                            apiNote.title,
                            apiNote.content,
                            apiNote.mongoId,
                            apiNote.version,
                            if (list.isEmpty()) 0 else list[list.size - 1].idx + 10000,
                            0,
                            false
                        )
                        list.add(RecyclerViewItem(newNote))
                        onNoteChangeHandler?.onAdd(list.size - 1)
                    }
                    return true
                } else {
                    Message.showErrorFromThread("Should not be called")
                }
                return false
            } else {
                Message.logE("$response")
                if (response.code() == 429) {
                    Message.showErrorFromThread("Too many requests, please try later.")
                } else if (response.code() == 401) {
                    Message.showErrorFromThread("Invalid credentials")
                    setLogout()
                } else if (response.code() == 500) {
                    Message.showErrorFromThread("Server internal error")
                } else {
                    Message.showErrorFromThread("Unexpected error")
                }
                return false
            }
        } catch (e: Exception) {
            Message.logW("Exception: $e")
            Message.showErrorFromThread("Cannot reach the server")
            return false
        }
    }

    private suspend fun setNoteAsRemovedOnServer(note: RecyclerViewItem) {
        val isConfirmedToRemove = onNoteChangeHandler?.onRemovedFromServer(note.title)
        if (isConfirmedToRemove != null && isConfirmedToRemove) {
            moveToTrashCan(note)
            onNoteChangeHandler?.onChange(removeFromList(note))
        } else {
            note.mongoId = ""
            SQLite.saveMongoId(note)
            SyncQueue.add(note)
        }
    }

    @JvmStatic
    fun moveToTrashCan(note: RecyclerViewItem) {
        removeFromList(note)
        val isRemovedFromServer = note.mongoId == null || note.mongoId.isEmpty()
        val time = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        note.toRemovedNote(time, isRemovedFromServer)
        SQLite.saveRemoval(note)
        if (!isRemovedFromServer) SyncQueue.add(note)
        removedList.add(note)
    }

    private fun findAndSaveAsRemoved(mongoId: String) {
        for (note in removedList) {
            if (note.mongoId == mongoId) {
                note.mongoId = ""
                note.isRemovedFromServer = true
                SQLite.saveRemoval(note)
                break
            }
        }
    }

    fun removeFromTrashCan(note: RecyclerViewItem) {
        note.removeTime -= 8 * 24 * 60 * 60 * 1000
        if (note.isRemovedFromServer) {
            for (i in removedList.indices) {
                if (removedList[i].id == note.id) {
                    removedList.removeAt(i)
                    SQLite.delete(note)
                    break
                }
            }
        }
    }

    fun restoreFromTrashCan(note: RecyclerViewItem) {
        note.removeTime = 0
        note.isRemovedFromServer = false
        SQLite.saveRemoval(note)
        for (i in removedList.indices) {
            if (removedList[i].id == note.id) {
                removedList.removeAt(i)
                break
            }
        }
        list.add(note)
        SyncQueue.add(note)
        onNoteChangeHandler?.onAdd(list.size - 1)
    }
}
