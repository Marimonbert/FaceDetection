package com.example.facedetection.presentation.utils

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

@Composable
fun FaceGuideOverlay(
    modifier: Modifier = Modifier,
    isFaceAligned: Boolean
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // 📏 Dimensões fixas do oval
        val ovalWidth = size.width * 0.6f // Ajusta para caber apenas o rosto
        val ovalHeight = size.height * 0.45f // Define altura ideal

        val topLeftX = centerX - (ovalWidth / 2)
        val topLeftY = centerY - (ovalHeight / 2)

        // 🎨 Cor muda para verde quando o rosto está alinhado
        val ovalColor = if (isFaceAligned) Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f)

        drawOval(
            color = ovalColor,
            topLeft = Offset(topLeftX, topLeftY),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 6.dp.toPx())
        )
    }
}



@androidx.annotation.OptIn(ExperimentalGetImage::class)
fun processFace(
    imageProxy: ImageProxy,
    onFaceDetected: (Boolean, String, Map<String, Any>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        Log.e("FaceDetection", "❌ mediaImage é NULL! Não foi possível processar.")
        imageProxy.close()
        onFaceDetected(false, "Erro ao acessar a imagem", emptyMap())
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(options)

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces.first()
                val boundingBox = face.boundingBox

                Log.d("FaceDetection", "📌 Rosto detectado! BoundingBox: $boundingBox")

                val extractedFeatures = mapOf(
                    "x" to boundingBox.left,
                    "y" to boundingBox.top,
                    "w" to boundingBox.width(),
                    "h" to boundingBox.height()
                )

                val isAligned = (face.headEulerAngleX in -10f..10f) &&
                        (face.headEulerAngleY in -10f..10f) &&
                        (face.headEulerAngleZ in -10f..10f) &&
                        (boundingBox.width() in 200..400) // 🔹 Ajustado para suportar variações

                val feedbackMessage = when {
                    boundingBox.width() < 200 -> "Aproxime-se mais"
                    boundingBox.width() > 400 -> "Afaste-se um pouco"
                    !isAligned -> "Alinhe seu rosto corretamente"
                    else -> "Rosto detectado corretamente!"
                }

                Log.d("FaceDetection", "📌 Alinhado: $isAligned, Feedback: $feedbackMessage")
                onFaceDetected(isAligned, feedbackMessage, extractedFeatures)
            } else {
                Log.d("FaceDetection", "❌ Nenhum rosto detectado")
                onFaceDetected(false, "Nenhum rosto detectado", emptyMap())
            }
        }
        .addOnFailureListener { e ->
            Log.e("FaceDetection", "⚠️ Erro ao processar rosto", e)
            onFaceDetected(false, "Erro ao processar rosto", emptyMap())
        }
        .addOnCompleteListener {
            Log.d("FaceDetection", "📌 Fechando imageProxy após processamento.")
            imageProxy.close() // 🔹 Só fecha a imagem DEPOIS de concluir o processamento.
        }
}





