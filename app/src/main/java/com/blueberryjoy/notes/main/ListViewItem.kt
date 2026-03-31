package com.blueberryjoy.notes.main

data class ListViewItem(
    var title: String,
    val action: (item: ListViewItem) -> Unit,
    val isShowingLambda: () -> Boolean
) {
    fun onClick() {
        action(this)
    }

    fun isShowing(): Boolean {
        return isShowingLambda()
    }
}
