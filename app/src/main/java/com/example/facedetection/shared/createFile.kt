package com.example.facedetection.shared

import android.content.Context
import com.example.facedetection.R
import java.io.File

fun createFile(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.getString(R.string.app_name)).apply { mkdirs() }
    }
    return File(mediaDir, "${System.currentTimeMillis()}.jpg")
}
