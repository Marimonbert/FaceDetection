package com.example.facedetection.data

class Models {

    companion object {

        val FACENET = ModelInfo(
            "FaceNet_512",
            "facenet_512.tflite", // 📌 Agora usamos o modelo 512
            cosineThreshold = 0.5f, // Ajustar threshold para maior precisão
            l2Threshold = 0.75f, // L2 mais rigoroso
            outputDims = 512, // 📌 Agora temos 512 dimensões em vez de 128
            inputDims = 160
        )
    }
}