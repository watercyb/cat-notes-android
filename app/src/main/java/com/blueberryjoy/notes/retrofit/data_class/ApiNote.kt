package com.blueberryjoy.notes.retrofit.data_class

import com.google.gson.annotations.SerializedName

data class ApiNote(
    @SerializedName("_id")
    val mongoId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("version")
    val version: Int,
    @SerializedName("isEncrypted")
    val isEncrypted: Boolean
) {
    override fun toString(): String {
        return "ApiNote(id=$mongoId, title='$title', version=$version, content='${
            content.replace(
                "\r\n",
                " "
            ).replace("\n", " ")
        }')"
    }
}