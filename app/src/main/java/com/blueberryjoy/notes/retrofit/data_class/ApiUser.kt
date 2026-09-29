package com.blueberryjoy.notes.retrofit.data_class

import com.google.gson.annotations.SerializedName

data class ApiUser(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("notes")
    val notes: Array<ApiNote>,
    @SerializedName("isVerified")
    val isVerified: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiUser

        if (isVerified != other.isVerified) return false
        if (name != other.name) return false
        if (email != other.email) return false
        if (!notes.contentEquals(other.notes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isVerified.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + notes.contentHashCode()
        return result
    }
}