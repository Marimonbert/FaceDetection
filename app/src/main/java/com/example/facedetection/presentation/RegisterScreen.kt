package com.example.facedetection.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.facedetection.data.FaceNetModel
import com.example.facedetection.data.Person
import com.example.facedetection.presentation.utils.BitmapUtils
import com.example.facedetection.shared.CameraCapture
import com.example.facedetection.storage.DatabaseHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    databaseHelper: DatabaseHelper,
    faceNetModel: FaceNetModel,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    if (capturedImageUri == null) {
        // 📸 Captura da imagem pela câmera
        CameraCapture(
            onImageCaptured = { uri ->
                Log.d("RegisterScreen", "Imagem capturada: $uri")
                capturedImageUri = uri
            },
            onError = { e ->
                Log.e("RegisterScreen", "Erro ao capturar imagem: ${e.message}")
                errorMessage = "Erro ao capturar imagem: ${e.message}"
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Exibição da imagem capturada
            Image(
                painter = rememberAsyncImagePainter(model = capturedImageUri),
                contentDescription = "Imagem Capturada",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            isProcessing = true
                            try {
                                capturedImageUri?.let { uri ->
                                    processAndSaveFace(
                                        context = context,
                                        uri = uri,
                                        name = name,
                                        databaseHelper = databaseHelper,
                                        faceNetModel = faceNetModel,
                                        onComplete = { success, message ->
                                            isProcessing = false
                                            if (success) {
                                                Toast.makeText(context, "Pessoa registrada!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            } else {
                                                errorMessage = message
                                            }
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                errorMessage = "Erro ao processar a imagem: ${e.message}"
                                Log.e("RegisterScreen", "Erro durante o registro: ${e.message}")
                                isProcessing = false
                            }
                        } else {
                            errorMessage = "Por favor, insira um nome antes de registrar."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text("Registrar")
                }
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
fun processAndSaveFace(
    context: Context,
    uri: Uri,
    name: String,
    databaseHelper: DatabaseHelper,
    faceNetModel: FaceNetModel,
    onComplete: (Boolean, String) -> Unit
) {
    val bitmap = BitmapUtils.getBitmapFromUri(context.contentResolver, uri)
    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    )

    val image = InputImage.fromBitmap(bitmap, 0)

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isEmpty()) {
                onComplete(false, "Nenhum rosto detectado!")
                return@addOnSuccessListener
            }

            val face = faces.first()
            val croppedFace = BitmapUtils.cropRectFromBitmap(bitmap, face.boundingBox)
            val resizedFace = Bitmap.createScaledBitmap(croppedFace, 160, 160, true)

            val embeddingArray = faceNetModel.getFaceEmbedding(resizedFace)

            if (embeddingArray.isEmpty()) {
                onComplete(false, "Erro ao gerar embedding facial.")
                return@addOnSuccessListener
            }

            val embeddingString = embeddingArray.joinToString(",")

            val person = Person(
                name = name,
                imageUri = uri.toString(),
                embedding = embeddingString
            )

            val id = databaseHelper.insertPerson(person)
            if (id > 0) {
                Log.d("RegisterScreen", "✅ Pessoa registrada: ${person.name} (ID: $id)")
                onComplete(true, "Registro bem-sucedido")
            } else {
                onComplete(false, "Erro ao salvar no banco de dados.")
            }
        }
        .addOnFailureListener { e ->
            onComplete(false, "Erro na detecção facial: ${e.message}")
        }
}
