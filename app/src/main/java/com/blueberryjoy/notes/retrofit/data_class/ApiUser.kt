package com.blueberryjoy.notes.retrofit.data_class

data class ApiUser(
    val name: String,
    val email: String,
    val notes: Array<ApiNote>,
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