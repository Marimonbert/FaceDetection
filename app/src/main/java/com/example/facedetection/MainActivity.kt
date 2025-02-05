package com.example.facedetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.example.facedetection.data.FaceNetModel
import com.example.facedetection.data.Models
import com.example.facedetection.presentation.utils.RequestPermissions
import com.example.facedetection.shared.AppNavigation
import com.example.facedetection.storage.DatabaseHelper
import com.example.facedetection.ui.theme.FaceDetectionTheme


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

    private lateinit var faceNetModel: FaceNetModel
    private lateinit var databaseHelper: DatabaseHelper

    private val useGpu = true
    private val useXNNPack = true
    private val modelInfo = Models.FACENET

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o banco de dados e o modelo FaceNet
        databaseHelper = DatabaseHelper(this)
        faceNetModel = FaceNetModel(this, modelInfo, useGpu, useXNNPack)

        setContent {
            FaceDetectionTheme {
                val context = LocalContext.current
                var hasPermissions by remember { mutableStateOf(false) }

                // 📌 Verifica a permissão sempre que a tela for recomposta
                LaunchedEffect(Unit) {
                    hasPermissions = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                }

                Scaffold(modifier = Modifier.fillMaxSize()) {
                    // Exibe tela de solicitação de permissão, se necessário
                    if (!hasPermissions) {
                        RequestPermissions(
                            permissions = arrayOf(Manifest.permission.CAMERA)
                        ) { granted -> hasPermissions = granted }
                    }

                    // Renderiza a navegação apenas após as permissões serem concedidas
                    if (hasPermissions) {
                        AppNavigation(databaseHelper, faceNetModel)
                    } else {
                        PermissionDeniedScreen()
                    }
                }
            }
        }
    }

}
@Composable
fun PermissionDeniedScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Permissões necessárias para continuar. Por favor, conceda as permissões nas configurações do app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}