package com.blueberryjoy.notes.retrofit.data_class

data class ApiUser(
    val name: String,
    val email: String,
    val notes: Array<ApiNote>,
    val isVerified: Boolean
)