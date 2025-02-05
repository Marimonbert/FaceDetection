package com.example.facedetection.data

import android.util.Log

data class Person(
    val id: Long = 0,
    val name: String,
    val imageUri: String,
    val embedding: String // Embeddings armazenados como String no banco de dados
) {
    fun getEmbeddingArray(): FloatArray {
        return try {
            embedding.split(",")
                .filter { it.isNotBlank() } // Remove valores vazios acidentais
                .map { it.trim().toFloat() } // Remove espaços extras e converte para Float
                .toFloatArray()
        } catch (e: NumberFormatException) {
            Log.e("Person", "Erro ao converter embedding para FloatArray: ${e.message}")
            FloatArray(0) // Retorna um array vazio em caso de erro
        }
    }
}
