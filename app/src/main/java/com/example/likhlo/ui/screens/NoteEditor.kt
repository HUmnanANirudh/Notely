@file:OptIn(InternalAPI::class)

package com.example.likhlo.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.likhlo.ui.model.Note
import com.example.likhlo.ui.theme.ButtonColor
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.InternalAPI
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun NoteEditor(
    navController: NavController,
    noteId: String? = null
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(noteId != null) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            isLoading = true
            val result = fetchNoteById(noteId, context)
            result.onSuccess {
                title = it.title
                content = it.content
            }.onFailure {
                errorMessage = it.message ?: "Failed to fetch note"
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = if (isEditing) "Edit Note" else "Create a New Note",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(48.dp)) // For balance
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    val result = if (noteId == null) {
                        createNote(title, content, context)
                    } else {
                        updateNote(noteId, title, content, context)
                    }

                    result.onSuccess {
                        Toast.makeText(
                            context,
                            if (noteId == null) "Note created!" else "Note updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack()
                    }.onFailure {
                        errorMessage = it.message ?: "An error occurred"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)
        ) {
            Text(text = if (noteId == null) "Create Note" else "Update Note")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
suspend fun createNote(title: String, content: String, context: Context): Result<Unit> {
    val token = getJwtToken(context) ?: return Result.failure(Exception("Token missing"))
    return try {
        val response = client.post("https://likhlo.shukurenai123.workers.dev/api/v1/notes/create") {
            headers { append("Authorization", "Bearer $token") }
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(Note(title = title, content = content)))
        }

        if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to create note: ${response.status}"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

suspend fun updateNote(noteId: String, title: String, content: String, context: Context): Result<Unit> {
    val token = getJwtToken(context) ?: return Result.failure(Exception("Token missing"))
    return try {
        val response = client.put("https://likhlo.shukurenai123.workers.dev/api/v1/notes/update/$noteId") {
            headers { append("Authorization", "Bearer $token") }
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(Note(id = noteId, title = title, content = content)))
        }

        if (response.status == HttpStatusCode.OK) Result.success(Unit)
        else Result.failure(Exception("Failed to update note: ${response.status}"))
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

suspend fun fetchNoteById(noteId: String, context: Context): Result<Note> {
    val token = getJwtToken(context) ?: return Result.failure(Exception("Token missing"))
    return try {
        val response = client.get("https://likhlo.shukurenai123.workers.dev/api/v1/notes/$noteId") {
            headers { append("Authorization", "Bearer $token") }
        }
        val body = response.bodyAsText()
        Result.success(Json.decodeFromString(body))
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}
