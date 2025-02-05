package com.example.facedetection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.facedetection.storage.DatabaseHelper

@Composable
fun MainScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reconhecimento Facial",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Registrar Pessoa")
        }

        Button(
            onClick = { navController.navigate("identify") },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Identificar Pessoa")
        }

        Button(
            onClick = { navController.navigate("peopleList") },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Pessoas Cadastradas")
        }
    }
}
