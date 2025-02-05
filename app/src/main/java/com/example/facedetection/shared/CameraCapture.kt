package com.example.facedetection.shared

import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.facedetection.presentation.utils.FaceGuideOverlay
import com.example.facedetection.presentation.utils.processFace
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    var isFaceAligned by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("Posicione seu rosto dentro do oval") }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(640, 480)) // 🔹 Define um tamanho compatível para ML Kit
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                Log.d("CameraCapture", "📷 Imagem recebida para análise") // ✅ Log de verificação

                try {
                    if (imageProxy.image == null) {
                        Log.e("CameraCapture", "❌ `imageProxy.image` é NULL!")
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    processFace(imageProxy) { isAligned, message, _ ->
                        Log.d("CameraCapture", "🔍 Rosto detectado: $isAligned, Mensagem: $message") // ✅ Log de verificação

                        isFaceAligned = isAligned
                        feedbackMessage = message
                    }
                } catch (e: Exception) {
                    Log.e("CameraCapture", "❌ Erro na análise da imagem", e)
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture)
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        FaceGuideOverlay(modifier = Modifier.fillMaxSize(), isFaceAligned = isFaceAligned)

        Text(
            text = feedbackMessage,
            color = if (isFaceAligned) Color.Green else Color.Red,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

        IconButton(
            onClick = {
                if (isFaceAligned) {
                    val photoFile = createFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exception: ImageCaptureException) {
                                onError(exception)
                            }

                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                                onImageCaptured(savedUri)
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                .padding(8.dp),
            enabled = isFaceAligned
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Capturar Imagem",
                tint = if (isFaceAligned) Color.White else Color.Gray,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
