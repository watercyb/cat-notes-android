package com.blueberryjoy.notes.retrofit.data_class

import com.google.gson.annotations.SerializedName

data class ApiReq(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("password")
    val password: String? = null,
    @SerializedName("noteId")
    val noteId: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("version")
    val version: Int? = null,
    @SerializedName("oldPassword")
    val oldPassword: String? = null,
    @SerializedName("newPassword")
    val newPassword: String? = null,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("refreshToken")
    val refreshToken: String? = null,
    @SerializedName("isEncrypted")
    val isEncrypted: Boolean? = null
)