package com.blueberryjoy.notes.retrofit.data_class

data class ApiRes(
    val success: Boolean,
    val message: String,
    val user: ApiUser
)