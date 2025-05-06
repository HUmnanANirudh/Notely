package com.example.likhlo.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.likhlo.R
import com.example.likhlo.ui.model.Note
import com.example.likhlo.ui.theme.ButtonColor
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Composable
fun Notes(navController: NavController) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshNotes() {
        coroutineScope.launch {
            isLoading = true
            val result = fetchNotesKtor(context)
            result.onSuccess {
                notes = it
            }.onFailure {
                errorMessage = it.message ?: "Unknown error"
            }
            isLoading = false
        }
    }
    val deleteNote: (String) -> Unit = { noteId ->
        coroutineScope.launch {
            isLoading = true
            val result = deleteNoteById(noteId, context)
            result.onSuccess {
                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                refreshNotes()
            }.onFailure {
                errorMessage = it.message ?: "Failed to delete note"
                Toast.makeText(context, "Failed to delete note", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }
    LaunchedEffect(Unit) {
        refreshNotes()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Rounded.Menu, contentDescription = "Menu")
            Text(
                text = "All Notes",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Icon(Icons.Rounded.Search, contentDescription = "Search")
        }
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ButtonColor)
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error loading notes", color = Color.Red)
                    Text(errorMessage ?: "", color = Color.Red)
                }
            }
            notes.isEmpty() -> {
                EmptyNotesState(navController)
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    NotesGrid(
                        notes = notes,
                        navController = navController,
                        onDelete = deleteNote
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { navController.navigate("note_editor") },
                            containerColor = ButtonColor
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Note",
                                tint = Color.White
                            )
                        }
                        FloatingActionButton(
                            onClick = { navController.navigate("Gemini") },
                            shape = CircleShape,
                            containerColor = ButtonColor
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Face,
                                contentDescription = "Ask Gemini",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun EmptyNotesState(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Create Your First Note",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add a note about anything (your thoughts on climate change, or your history essay) and share it with the world.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.notes),
                contentDescription = "Notes",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            Button(
                onClick = { navController.navigate("note_editor") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "CREATE A NOTE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NotesGrid(
    notes: List<Note>,
    navController: NavController,
    onDelete: (String) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.padding(16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes) { note ->
            NoteCard(
                note = note,
                navController = navController,
                onDelete = onDelete
            )
        }
    }
}
@Composable
fun NoteCard(
    note: Note,
    navController: NavController,
    onDelete: (String) -> Unit
) {
    val randomHeight = remember { (150..250).random().dp }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(randomHeight)
            .clickable {
                if (note.id != null) {
                    try {
                        navController.navigate("note_editor/${note.id}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error opening note: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Cannot edit note with missing ID", Toast.LENGTH_SHORT).show()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true
                    },
                    onTap = {
                        navController.navigate("note_editor/${note.id}")
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(note.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(note.content, maxLines = 4, color = Color.Gray)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete '${note.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        note.id?.let { onDelete(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

suspend fun fetchNotesKtor(context: Context): Result<List<Note>> {
    val token = getJwtToken(context) ?: return Result.failure(Exception("Token missing"))

    return try {
        val response = client.get("https://likhlo.shukurenai123.workers.dev/api/v1/notes/all") {
            headers { append("Authorization", "Bearer $token") }
        }

        val responseText = response.bodyAsText()
        println("Notes API response: $responseText")
        if (response.status.isSuccess()) {
            try {
                val json = Json.parseToJsonElement(responseText).jsonObject
                if (json.containsKey("Notes")) {
                    val notesJson = json["Notes"]
                    val notes = Json.decodeFromJsonElement<List<Note>>(notesJson!!)
                    Result.success(notes)
                } else {
                    println("Response doesn't contain Notes field: $responseText")
                    Result.success(emptyList())
                }
            } catch (e: Exception) {
                println("JSON parsing error: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("API error: ${response.status}"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}
suspend fun deleteNoteById(noteId: String, context: Context): Result<Unit> {
    val token = getJwtToken(context) ?: return Result.failure(Exception("Token missing"))
    return try {
        val response = client.delete("https://likhlo.shukurenai123.workers.dev/api/v1/notes/${noteId}") {
            headers { append("Authorization", "Bearer $token") }
        }

        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception("Failed to delete note: ${response.status}. $errorBody"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}
fun getJwtToken(context: Context): String? {
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    return prefs.getString("jwt_token", null)
}
