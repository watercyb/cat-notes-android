package com.blueberryjoy.notes.retrofit

import com.blueberryjoy.notes.auth.AuthManager.setLogout
import com.blueberryjoy.notes.retrofit.data_class.ApiRes
import com.blueberryjoy.notes.utils.Message

object ErrorHandler {
    fun res(response: retrofit2.Response<ApiRes>) {
        Message.logW("$response")
        if (response.code() == 401) {
            Message.invalidCredentials()
            setLogout()
        } else if (response.code() == 404) {
            Message.internalError()
        } else if (response.code() == 422) {
            Message.allFieldsRequired()
        } else if (response.code() == 429) {
            Message.tooManyRequests()
        } else if (response.code() == 500) {
            Message.internalError()
        } else if (response.code() == 503) {
            Message.internalError()
        } else if (response.code() == 521) {
            Message.cannotReachServer()
        } else {
            Message.unexpectedError()
        }
    }
}