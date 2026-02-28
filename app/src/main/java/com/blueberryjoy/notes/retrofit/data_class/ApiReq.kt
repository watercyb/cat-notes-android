package com.blueberryjoy.notes.retrofit.data_class

data class ApiReq(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val noteId: String? = null,
    val title: String? = null,
    val content: String? = null,
    val version: Int? = null,
    val oldPassword: String? = null,
    val newPassword: String? = null,
    val code: String? = null
)