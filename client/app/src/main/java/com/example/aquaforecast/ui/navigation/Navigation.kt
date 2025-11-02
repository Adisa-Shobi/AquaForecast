package com.example.aquaforecast.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.ui.auth.AuthViewModel
import com.example.aquaforecast.ui.auth.LoginScreen
import com.example.aquaforecast.ui.splash.SplashScreen
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.koinInject

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, Screen.Splash.route) {
        composable(route = Screen.Login.route) {
            val authViewModel: AuthViewModel = koinInject()
            LoginScreen({
                navController.navigate(Screen.Prediction.route) {
                    popUpTo(Screen.Login.route) {
                        inclusive=true
                    }
                }
            }, authViewModel)
        }
        composable(route = Screen.Splash.route) {
            SplashScreen({
                navController.navigate( Screen.Login.route) {
                    popUpTo(Screen.Splash.route) {
                        inclusive=true
                    }
                }
            }, {
                navController.navigate( Screen.Prediction.route) {
                    popUpTo(Screen.Splash.route) {
                        inclusive=true
                    }
                }
            })
        }
        composable(route = Screen.Prediction.route) {
            PredictionScreen()
        }
    }
}

@Composable
fun PredictionScreen () {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column (
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Prediction Screen",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}