package com.blueberryjoy.notes.retrofit.data_class

import com.google.gson.annotations.SerializedName

data class ApiRes(
    @SerializedName("success")
    val success: Boolean?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("user")
    val user: ApiUser?,
    @SerializedName("note")
    val note: ApiNote?,
    @SerializedName("noteFromServer")
    val noteFromServer: ApiNote?,
    @SerializedName("newNote")
    val newNote: ApiNote?,
    @SerializedName("notes")
    val notes: Array<ApiNote>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiRes

        if (success != other.success) return false
        if (message != other.message) return false
        if (user != other.user) return false
        if (note != other.note) return false
        if (noteFromServer != other.noteFromServer) return false
        if (newNote != other.newNote) return false
        if (!notes.contentEquals(other.notes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = success?.hashCode() ?: 0
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (user?.hashCode() ?: 0)
        result = 31 * result + (note?.hashCode() ?: 0)
        result = 31 * result + (noteFromServer?.hashCode() ?: 0)
        result = 31 * result + (newNote?.hashCode() ?: 0)
        result = 31 * result + notes.contentHashCode()
        return result
    }
}