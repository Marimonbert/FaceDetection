package com.example.facedetection.presentation

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.facedetection.data.FaceNetModel
import com.example.facedetection.data.Person
import com.example.facedetection.presentation.utils.FaceGuideOverlay
import com.example.facedetection.shared.CameraPreview
import com.example.facedetection.storage.DatabaseHelper
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IdentifyScreen(
    navController: NavHostController,
    databaseHelper: DatabaseHelper,
    faceNetModel: FaceNetModel
) {
    val context = LocalContext.current
    var recognizedPerson by remember { mutableStateOf<Person?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    var isFaceAligned by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("Posicione seu rosto dentro do oval") }

    val frameAnalyser by rememberUpdatedState(
        FrameAnalyser(
            context,
            databaseHelper,
            faceNetModel,
            onFacePositionUpdate = { aligned, message ->
                isFaceAligned = aligned
                feedbackMessage = message
            },
            onPersonRecognized = { person ->
                if (!isNavigating && isFaceAligned) {
                    recognizedPerson = person
                }
            }
        )
    )

    LaunchedEffect(recognizedPerson) {
        if (recognizedPerson != null && !isNavigating) {
            isNavigating = true
            val encodedUri = Uri.encode(recognizedPerson!!.imageUri)
            Log.d("IdentifyScreen", "✅ Pessoa reconhecida: ${recognizedPerson!!.name}, URI: $encodedUri")

            navController.navigate("welcome/${recognizedPerson!!.name}/$encodedUri") {
                popUpTo("identify") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            onAnalyzerReady = { imageAnalysis ->
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser)
            }
        )

        // 🔹 **Desenha o oval de alinhamento**
        FaceGuideOverlay(
            modifier = Modifier.fillMaxSize(),
            isFaceAligned = isFaceAligned
        )

        // 🔹 **Mensagem de feedback**
        Text(
            text = feedbackMessage,
            color = if (isFaceAligned) Color.Green else Color.Red,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
    }
}

