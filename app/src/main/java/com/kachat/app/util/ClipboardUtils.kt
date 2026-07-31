package com.kachat.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Copies sensitive text (a private key hex) to the system clipboard and auto-wipes it 30 seconds
 * later — but only if the clipboard still holds that exact value (i.e. the user hasn't copied
 * something else in the meantime). A main-looper [Handler] is used rather than a composition-scoped
 * coroutine so the wipe still fires after the user navigates away from the screen that copied it.
 * Mirrors the iOS 30-second clipboard clear for private-key material.
 *
 * Seed phrases are deliberately NOT copyable anywhere and must never be passed to this helper —
 * they can only be transcribed by hand.
 */
fun copyPrivateKeyWithAutoWipe(context: Context, value: String, label: String = "private key") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Handler(Looper.getMainLooper()).postDelayed({
        val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        if (current == value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }, 30_000L)
}
