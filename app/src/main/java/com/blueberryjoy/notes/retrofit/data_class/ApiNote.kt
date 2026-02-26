package com.blueberryjoy.notes.retrofit.data_class

import com.google.gson.annotations.SerializedName

data class ApiNote(
    @SerializedName("_id")
    val mongoId: String,
    val title: String?,
    val content: String?,
    val version: Int
) {
    override fun toString(): String {
        return "ApiNote(id=$mongoId, title='$title', version=$version, content='${
            content?.replace(
                "\r\n",
                " "
            )?.replace("\n", " ")
        }')"
    }
}