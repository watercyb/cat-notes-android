package com.blueberryjoy.notes.editor

import androidx.lifecycle.ViewModel
import com.blueberryjoy.notes.utils.RecyclerViewItem
import javax.crypto.spec.SecretKeySpec

class EditorViewModel : ViewModel() {
    var id: Int = -1
    var title: String = ""
    var content: String = ""
    var isSaved = false
    var originContent: String? = null
    var note: RecyclerViewItem? = null
    var salt: ByteArray? = null
    var key: SecretKeySpec? = null
}