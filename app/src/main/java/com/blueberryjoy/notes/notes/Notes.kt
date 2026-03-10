package com.blueberryjoy.notes.notes

import android.os.Handler
import android.os.Looper
import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthType
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.retrofit.ErrorHandler
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
                    Message.logW("${removedList[removedList.size - 1]}")
                }
            }
        }
        //for (note in list) {
        //note.userId = ""
        //}
        list.sortWith(Comparator.comparingInt(ToIntFunction { a: RecyclerViewItem? -> a!!.idx }))
        for (i in list.indices) {
            list[i].idx = i * 10000
        }
        this.onNoteChangeHandler = onNoteChangeHandler
    }

    @JvmStatic
    fun getOnNoteChangeHandler(): OnNoteChangeHandler? {
        return onNoteChangeHandler
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
        isRemovedFromServer: Boolean,
        userId: String
    ): Int {
        val idx = (if (list.isEmpty()) 0 else list[list.size - 1].idx) + 10000
        val note = SQLite.create(
            title, content, mongoId, version, idx, removeTime, isRemovedFromServer, userId
        )
        val item = RecyclerViewItem(note)
        onNoteChangeHandler?.onAdd(item)
        return item.id
    }

    //Create Note from the API result.
    fun create(note: ApiNote): Int {
        return create(
            note.title, note.content, note.mongoId, note.version, 0, false, AuthManager.getUserId()
        )
    }

    fun updateNote(notes: Array<ApiNote>?) {
        if (notes == null) return

        SyncQueue.setIsUpdatingNotesFromApi(true)
        val mongoIdToNotes: HashMap<String, RecyclerViewItem> = HashMap()

        for (note in list) {
            if (note.userId.isEmpty()) {
                note.userId = AuthManager.getUserId()
                SQLite.saveUserId(note)
            } else if (note.userId != AuthManager.getUserId()) {
                continue
            }

            if (note.mongoId == null || note.mongoId.isEmpty()) {
                SyncQueue.add(note)
            } else {
                mongoIdToNotes[note.mongoId] = note
            }
        }

        for (note in removedList) {
            if (note.userId.isEmpty()) {
                note.userId = AuthManager.getUserId()
                SQLite.saveUserId(note)
            } else if (note.userId != AuthManager.getUserId()) {
                continue
            }
            if (note.mongoId != null && !note.mongoId.isEmpty()) mongoIdToNotes[note.mongoId] = note
        }

        for (note in notes) {
            val recNote = mongoIdToNotes[note.mongoId]
            if (recNote != null) {
                compareNoteAndUpdateWithSameMongoId(recNote, note)
                mongoIdToNotes.remove(note.mongoId)
            } else {
                create(note)
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
            onNoteChangeHandler?.onChange(note)
        } else if (!note.equals(apiNote)) {
            create(apiNote)
            note.mongoId = ""
            SyncQueue.add(note)
            SQLite.saveMongoId(note)
        }
    }

    suspend fun createNoteOnApi(note: RecyclerViewItem): Boolean {
        if (!checkAndGetToken()) return false

        val apiReq = ApiReq(title = note.title, content = note.content, version = note.version)
        try {
            val response = RetrofitInstance.api.createNote(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    note.mongoId = res.note?.mongoId
                    SQLite.saveMongoId(note)
                    return true
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (e: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun deleteNodeFromApi(mongoId: String): Boolean {
        if (!checkAndGetToken()) return false

        try {
            val response = RetrofitInstance.api.deleteNote(mongoId)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.logW("Note deleted")
                    findAndSaveAsRemoved(mongoId)
                    return true
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (e: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun updateNoteToApi(note: RecyclerViewItem): Boolean {
        if (!checkAndGetToken()) return false

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
                        Message.unexpectedError()
                        return false
                    }
                    SQLite.saveMongoId(note)
                    return true
                } else if (res?.noteFromServer != null && res.newNote != null) {
                    note.mongoId = res.newNote.mongoId
                    SQLite.saveMongoId(note)
                    create(res.noteFromServer)
                    Message.show(
                        App.getInstance().getContext().getString(R.string.newer_version_found)
                    )
                    return true
                } else {
                    Message.debug("updateNoteToApi: Should not be called")
                }
                return false
            } else {
                if (response.code() == 404) {
                    setNoteAsRemovedOnServer(note)
                } else {
                    ErrorHandler.res(response)
                }
                return false
            }
        } catch (e: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun getNotesFromApi(): Boolean {
        if (!checkAndGetToken()) return false

        try {
            val response = RetrofitInstance.api.getNotesIdAndVersion()
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    updateNoteWithoutTitleAndContents(res.notes)
                    return true
                } else {
                    Message.debug("getNotesFromApi: Should not be called")
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (e: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    fun updateNoteWithoutTitleAndContents(notes: Array<ApiNote>?) {
        if (notes == null) return

        SyncQueue.setIsUpdatingNotesFromApi(true)
        val mongoIdToNotes: HashMap<String, RecyclerViewItem> = HashMap()

        for (note in list) {
            if (note.userId.isEmpty()) {
                note.userId = AuthManager.getUserId()
                SQLite.saveUserId(note)
            } else if (note.userId != AuthManager.getUserId()) {
                continue
            }
            if (note.mongoId == null || note.mongoId.isEmpty()) {
                SyncQueue.add(note)
            } else {
                mongoIdToNotes[note.mongoId] = note
            }
        }

        for (note in removedList) {
            if (note.userId.isEmpty()) {
                note.userId = AuthManager.getUserId()
                SQLite.saveUserId(note)
            } else if (note.userId != AuthManager.getUserId()) {
                continue
            }
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
        if (!checkAndGetToken()) return false

        try {
            val response = RetrofitInstance.api.getNote(mongoId)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    //If note is not on the server, return true.
                    val apiNote = res.note ?: return true
                    val idx = getNoteByMongoId(apiNote.mongoId)
                    if (idx >= 0) {
                        val recNote = list[idx]
                        compareNoteAndUpdateWithSameMongoId(recNote, apiNote)
                    } else {
                        create(apiNote)
                    }
                    return true
                } else {
                    Message.debug("getNoteFromApi: Should not be called")
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (e: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    private suspend fun setNoteAsRemovedOnServer(note: RecyclerViewItem) {
        val isConfirmedToRemove = onNoteChangeHandler?.onRemovedFromServer(note.title)
        if (isConfirmedToRemove != null && isConfirmedToRemove) {
            moveToTrashCan(note)
        } else if (note.userId == AuthManager.getUserId()) {
            note.mongoId = ""
            SQLite.saveMongoId(note)
            SyncQueue.add(note)
        }
    }

    @JvmStatic
    fun moveToTrashCan(note: RecyclerViewItem) {
        onNoteChangeHandler?.onRemove(note)
        val isRemovedFromServer = note.mongoId == null || note.mongoId.isEmpty()
        val time = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        note.toRemovedNote(time, isRemovedFromServer)
        SQLite.saveRemoval(note)
        if (!isRemovedFromServer && note.userId == AuthManager.getUserId()) {
            Handler(Looper.getMainLooper()).postDelayed({
                SyncQueue.add(note)
            }, 500)
        }
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
            val idx = getRemovedNoteById(note.id)
            if (idx >= 0) removedList.removeAt(idx)
            SQLite.delete(note)
        } else {
            SQLite.saveRemoval(note)
        }
    }

    fun restoreFromTrashCan(note: RecyclerViewItem) {
        note.removeTime = 0
        note.isRemovedFromServer = false
        SQLite.saveRemoval(note)
        val idx = getRemovedNoteById(note.id)
        if (idx >= 0) removedList.removeAt(idx)
        if (note.userId == AuthManager.getUserId()) SyncQueue.add(note)
        onNoteChangeHandler?.onAdd(note)
    }

    private suspend fun checkAndGetToken(): Boolean {
        return if (AuthManager.getType() == AuthType.EMAIL) {
            !AuthManager.getToken().isEmpty() || !AuthManager.getRefreshToken().isEmpty()
        } else if (AuthManager.getType() == AuthType.GOOGLE) {
            ClerkAuth.getToken()
        } else {
            false
        }
    }

    private fun getRemovedNoteById(id: Int): Int {
        for (i in removedList.indices) {
            if (removedList[i].id == id) {
                return i
            }
        }
        return -1
    }
}
