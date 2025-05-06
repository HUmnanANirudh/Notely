package com.example.likhlo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.likhlo.ui.screens.Login
import com.example.likhlo.ui.screens.NoteEditor
import com.example.likhlo.ui.screens.Notes
import com.example.likhlo.ui.screens.Signup
import com.example.likhlo.ui.screens.Welcome

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "welcome"
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
        composable("note_editor") {
            NoteEditor(navController = navController, noteId = null)
        }
        composable("note_editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteEditor(navController = navController, noteId = noteId)
        }
    }
}


