package com.forseti.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.forseti.R

/**
 * Puts plain text on the system clipboard and shows a short toast so the user
 * knows the gesture succeeded (especially important for OCR-derived PDF text,
 * which has no selectable text layer in the viewer).
 */
fun Context.copyPlainText(label: String, text: String, showToast: Boolean = true) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    if (showToast) {
        Toast.makeText(this, getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
}
