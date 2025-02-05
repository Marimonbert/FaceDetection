package com.example.facedetection.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.facedetection.data.FaceNetModel
import com.example.facedetection.data.Person
import com.example.facedetection.presentation.utils.BitmapUtils
import com.example.facedetection.storage.DatabaseHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

class FrameAnalyser(
    private val context: Context,
    private val databaseHelper: DatabaseHelper,
    private val faceNetModel: FaceNetModel,
    private val onFacePositionUpdate: (Boolean, String) -> Unit, // 🔹 Para feedback do rosto
    private val onPersonRecognized: (Person?) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    )

    private var isProcessing = false
    private var subject = FloatArray(512)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }
        isProcessing = true

        val mediaImage = image.image
        if (mediaImage != null) {
            val frameBitmap = BitmapUtils.imageToBitmap(
                mediaImage,
                image.imageInfo.rotationDegrees,
                isFrontCamera = true
            )

            processImage(frameBitmap, image)
        }
    }

    private fun processImage(bitmap: Bitmap, image: ImageProxy) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    Log.d("FrameAnalyser", "❌ Nenhum rosto detectado")
                    onFacePositionUpdate(false, "Nenhum rosto detectado")
                    isProcessing = false
                    image.close()
                    return@addOnSuccessListener
                }

                val face = faces.first()
                val boundingBox = face.boundingBox

                // 📏 Parâmetros do oval
                val minWidth = 180 // 🔹 Antes: 200 → Agora: 180 (permite rostos um pouco menores)
                val maxWidth = 420 // 🔹 Antes: 400 → Agora: 420 (permite rostos um pouco maiores)

// 📌 Permitir maior inclinação da cabeça
                val isAligned = boundingBox.width() in minWidth..maxWidth &&
                        (face.headEulerAngleX in -15f..15f) &&  // 🔹 Antes: -10f..10f → Agora: -15f..15f
                        (face.headEulerAngleY in -15f..15f) &&  // 🔹 Permitir leve rotação lateral
                        (face.headEulerAngleZ in -15f..15f)     // 🔹 Pequena inclinação aceitável


                val feedbackMessage = when {
                    boundingBox.width() < minWidth -> "Aproxime-se mais"
                    boundingBox.width() > maxWidth -> "Afaste-se um pouco"
                    !isAligned -> "Mantenha seu rosto reto e nivelado"
                    else -> "Perfeito! Rosto alinhado!"
                }

                onFacePositionUpdate(isAligned, feedbackMessage)

                if (!isAligned) {
                    Log.d("FrameAnalyser", "❌ Rosto não está alinhado corretamente.")
                    isProcessing = false
                    image.close()
                    return@addOnSuccessListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    handleRecognition(face, bitmap, image)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FrameAnalyser", "❌ Erro ao processar rosto: ${e.message}")
                isProcessing = false
                image.close()
            }
    }

    private suspend fun handleRecognition(face: Face, bitmap: Bitmap, image: ImageProxy) {
        withContext(Dispatchers.Main) {
            try {
                val croppedBitmap = BitmapUtils.cropRectFromBitmap(bitmap, face.boundingBox)

                // 🔥 Aplica normalização de brilho ANTES de redimensionar
                val adjustedBitmap = normalizeBrightness(croppedBitmap)

                val resizedBitmap = Bitmap.createScaledBitmap(adjustedBitmap, 160, 160, true)

                val embeddingRaw = faceNetModel.getFaceEmbedding(resizedBitmap)

                if (embeddingRaw.isEmpty()) {
                    Log.e("FrameAnalyser", "⚠️ Embedding vazia gerada! Pulando...")
                    return@withContext
                }

                subject = normalizeEmbedding(embeddingRaw)
                val bestMatch = findBestMatch(subject)

                onPersonRecognized(bestMatch)
            } catch (e: Exception) {
                Log.e("FrameAnalyser", "❌ Erro ao processar face: ${e.message}")
            } finally {
                isProcessing = false
                image.close()
            }
        }
    }



    private fun findBestMatch(embedding: FloatArray): Person? {
        val persons = databaseHelper.getAllPersons()
        if (persons.isEmpty()) return null

        var bestMatch: Person? = null
        var bestL2Score = Float.MAX_VALUE
        var bestCosineScore = Float.MIN_VALUE

        for (person in persons) {
            val storedEmbedding = normalizeEmbedding(person.getEmbeddingArray())

            val l2Distance = L2Norm(embedding, storedEmbedding)
            val cosineSimilarity = cosineSimilarity(embedding, storedEmbedding)

            Log.d(
                "FrameAnalyser",
                "📌 Comparando com: ${person.name}, L2: $l2Distance, Cosine: $cosineSimilarity"
            )

            if (l2Distance < bestL2Score) {
                bestL2Score = l2Distance
                bestMatch = person
            }
            if (cosineSimilarity > bestCosineScore) {
                bestCosineScore = cosineSimilarity
            }
        }

        Log.d(
            "FrameAnalyser",
            "🎯 Melhor match: ${bestMatch?.name}, L2: $bestL2Score, Cosine: $bestCosineScore"
        )

        return if (bestL2Score <= 0.75f || bestCosineScore >= 0.6f) bestMatch else null
    }

    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.fold(0f) { acc, value -> acc + value * value })
        return if (norm == 0f) {
            Log.e("FrameAnalyser", "⚠️ Normalização falhou: norm=0")
            FloatArray(embedding.size) { 0f }
        } else {
            FloatArray(embedding.size) { i -> embedding[i] / norm }
        }
    }

    private fun L2Norm(x1: FloatArray, x2: FloatArray): Float {
        return sqrt(x1.zip(x2) { a, b -> (a - b).pow(2) }.sum())
    }

    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val dotProduct = x1.zip(x2) { a, b -> a * b }.sum()
        val norm1 = sqrt(x1.fold(0f) { acc, value -> acc + value * value })
        val norm2 = sqrt(x2.fold(0f) { acc, value -> acc + value * value })

        if (norm1 == 0f || norm2 == 0f) {
            Log.e("FrameAnalyser", "⚠️ Divisão por zero na similaridade cosseno!")
            return -1.0f
        }

        return dotProduct / (norm1 * norm2)
    }

    // 🔹 Normalizar brilho e contraste automaticamente
    private fun normalizeBrightness(bitmap: Bitmap): Bitmap {
        val bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bmp)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        // 🔥 Ajuste dinâmico do brilho baseado na média da imagem
        val avgBrightness = calculateAverageBrightness(bitmap)

        if (avgBrightness < 80) {
            // 📌 Se a imagem estiver muito escura, aumentamos o brilho
            colorMatrix.setScale(1.3f, 1.3f, 1.3f, 1f)
        } else if (avgBrightness > 180) {
            // 📌 Se a imagem estiver muito clara, reduzimos o brilho
            colorMatrix.setScale(0.8f, 0.8f, 0.8f, 1f)
        }

        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(bmp, 0f, 0f, paint)

        return bmp
    }

    // 🔹 Função para calcular a média do brilho da imagem
    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        var totalBrightness = 0L
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val brightness = (r + g + b) / 3  // Média dos canais de cor
            totalBrightness += brightness
        }

        return totalBrightness / pixels.size.toFloat()
    }

}
