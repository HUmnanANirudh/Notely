package com.example.likhlo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.example.likhlo.ui.screens.Login
import com.example.likhlo.ui.screens.Notes
import com.example.likhlo.ui.screens.Signup
import com.example.likhlo.ui.screens.Welcome

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "notes"
    ) {
        composable("welcome") {
            Welcome(navController)
        }
        composable("login") {
            Login(navController)
        }
        composable("signup") {
            Signup(navController)
        }
        composable("notes") {
            Notes(navController)
        }
    }
}


