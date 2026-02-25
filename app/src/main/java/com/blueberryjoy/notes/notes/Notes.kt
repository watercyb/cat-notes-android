package com.blueberryjoy.notes.notes

import android.util.Log
import android.widget.Toast
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthManager.setLogout
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.main.RecyclerViewItem
import com.blueberryjoy.notes.utils.Message
import com.blueberryjoy.notes.retrofit.RetrofitInstance
import com.blueberryjoy.notes.retrofit.data_class.ApiNote
import com.blueberryjoy.notes.retrofit.data_class.ApiReq
import com.blueberryjoy.notes.sqlite.SQLite
import com.blueberryjoy.notes.auth.AuthType
import java.util.function.ToIntFunction

object Notes {
    @JvmStatic
    var list: MutableList<RecyclerViewItem> = ArrayList()
        private set
    private var onNoteChangeHandler: OnNoteChangeHandler? = null

    fun initialize(onNoteChangeHandler: OnNoteChangeHandler) {
        val notes = SQLite.initialize()
        list = ArrayList()
        for (note in notes) {
            list.add(RecyclerViewItem(note))
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

    fun create(title: String?, content: String?, mongoId: String?, version: Int): Int {
        try {
            val idx = (if (list.isEmpty()) 0 else list[list.size - 1].idx) + 10000
            val note = SQLite.create(title, content, mongoId, version, idx)
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
            val note = SQLite.create(title, content, mongoId, version, idx)
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
    fun remove(note: RecyclerViewItem) {
        for (i in list.indices) {
            if (list[i].id == note.id) {
                list.removeAt(i)
                SyncQueue.add(note)
                break
            }
        }
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
            } else if (!note.mongoId.equals("REMOVED FROM SERVER")) {
                mongoIdToNotes[note.mongoId] = note
            }
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
                    if (list.isEmpty()) 0 else list[list.size - 1].idx + 10000
                )
                list.add(RecyclerViewItem(newNote))
                onNoteChangeHandler?.onAdd(list.size - 1)
            }
        }

        for (note in mongoIdToNotes.values) {
            note.title += " (Removed from server)"
            note.mongoId = "REMOVED FROM SERVER"
            SQLite.saveNote(note)
            onNoteChangeHandler?.onChange(getNoteById(note.id))
        }
        SyncQueue.setIsUpdatingNotesFromApi(false)
    }

    fun compareNoteAndUpdateWithSameMongoId(note: RecyclerViewItem, apiNote: ApiNote) {
        if (note.version > apiNote.version) {
            SyncQueue.add(note)
        } else if (note.version < apiNote.version || !note.equals(apiNote)) {
            val newNote = SQLite.create(
                apiNote.title,
                apiNote.content,
                apiNote.mongoId,
                apiNote.version,
                if (list.isEmpty()) 0 else list[list.size - 1].idx + 10000
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
                    } else if (res.noteFromServer != null && res.newNote != null) {
                        note.mongoId = res.newNote.mongoId
                        SQLite.saveMongoId(note)
                        create(res.noteFromServer)
                        onNoteChangeHandler?.onAdd(list.size - 1)
                    } else {
                        Message.showErrorFromThread("Unexpected error")
                        return false
                    }
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
                }
                return false
            }
        } catch (e: Exception) {
            Message.logW("Exception: $e")
            Message.showErrorFromThread("Cannot reach the server")
            return false
        }
    }
}
