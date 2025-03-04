package com.example.lab1.ui.first_fragment

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.shareImageToInstagramStories(uri: Uri): Boolean {
    return try {
        val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY")
        storiesIntent.setDataAndType(uri, "image/*")
        storiesIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        storiesIntent.setPackage("com.instagram.android")
        this.grantUriPermission(
            "com.instagram.android",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        this.startActivity(storiesIntent, null)
        true
    } catch (e: Exception) {
        false
    }
}