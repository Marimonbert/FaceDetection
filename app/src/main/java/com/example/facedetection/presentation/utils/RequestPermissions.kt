package com.example.facedetection.presentation.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun RequestPermissions(
    permissions: Array<String>,
    onPermissionsResult: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val permissionsGranted = remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Lançador para solicitar permissões
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted.value = result.values.all { it }
        onPermissionsResult(permissionsGranted.value)
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val activity = context as Activity
            val allGranted = permissions.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                permissionsGranted.value = true
                onPermissionsResult(true)
            } else {
                launcher.launch(permissions)
            }
        }
    }
}
