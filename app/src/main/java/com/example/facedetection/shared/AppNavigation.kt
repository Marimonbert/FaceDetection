package com.example.facedetection.shared

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.facedetection.MainScreen
import com.example.facedetection.data.FaceNetModel
import com.example.facedetection.data.Person
import com.example.facedetection.presentation.IdentifyScreen
import com.example.facedetection.presentation.PeopleListScreen
import com.example.facedetection.presentation.RegisterScreen
import com.example.facedetection.presentation.WelcomeScreen
import com.example.facedetection.storage.DatabaseHelper

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(databaseHelper: DatabaseHelper, faceNetModel: FaceNetModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        // Tela principal
        composable("main") {
            MainScreen(navController = navController)
        }

        // Tela de identificação
        composable("identify") {
            IdentifyScreen(navController = navController, databaseHelper = databaseHelper, faceNetModel = faceNetModel)
        }

        // Tela de registro
        composable("register") {
            RegisterScreen(
                navController = navController,
                databaseHelper = databaseHelper,
                faceNetModel = faceNetModel,
            )
        }

        // Tela de registro
        composable("peopleList") {
            PeopleListScreen(navController = navController, databaseHelper = databaseHelper)
        }


        // Tela de boas-vindas (agora passa nome e imagem!)
        composable(
            "welcome/{name}/{imageUri}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""

            WelcomeScreen(
                person = Person(name = name, imageUri = imageUri, embedding = ""),
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}
