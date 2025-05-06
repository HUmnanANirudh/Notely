package com.example.likhlo.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.likhlo.ui.theme.ButtonColor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SignupRequest(
    val email: String,
    @SerialName("Username")
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(val jwt: String)

val signupClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
    }
}

suspend fun signupRequest(username: String, email: String, password: String): Result<String> {
    return try {
        val response = signupClient.post("https://likhlo.shukurenai123.workers.dev/api/v1/users/SignUp") {
            contentType(ContentType.Application.Json)
            setBody(SignupRequest(email, username, password))
        }
        val responseText = response.bodyAsText()
        if (responseText.contains("User already exists")) {
            return Result.failure(Exception("User already exists with this email or username"))
        }
        try {
            val data: AuthResponse = Json.decodeFromString(responseText)
            Result.success(data.jwt)
        } catch (e: Exception) {
            val jwtRegex = "\"jwt\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val matchResult = jwtRegex.find(responseText)
            val jwt = matchResult?.groupValues?.get(1)

            if (jwt != null) {
                Result.success(jwt)
            } else {
                throw Exception("Failed to extract JWT from response: $responseText")
            }
        }
    } catch (e: Exception) {
        println("Signup error: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}

@Composable
fun Signup(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signupError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.White,
        cursorColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black,
        focusedTextColor = Color.Black,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LikhLo",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            )
            Spacer(modifier = Modifier.height(36.dp))
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Create a free account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Join LikhLo for free. Create and share unlimited notes with your friends.",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
            if (signupError.isNotEmpty()) {
                Text(
                    text = signupError,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    scope.launch {
                        if (email.isBlank() || password.isBlank() || username.isBlank()) {
                            Toast.makeText(context, "Please ensure all fields are filled", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        isLoading = true
                        signupError = ""
                        try {
                            val result = signupRequest(username, email, password)
                            result.onSuccess { jwt ->
                                val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                                sharedPrefs.edit().putString("jwt_token", jwt).apply()
                                Toast.makeText(context, "Signup successful!", Toast.LENGTH_SHORT).show()
                                navController.navigate("notes")
                            }.onFailure { throwable ->
                                if (throwable.message?.contains("User already exists") == true) {
                                    signupError = "User already exists with this email or username"
                                    Toast.makeText(context, signupError, Toast.LENGTH_LONG).show()
                                }else if (throwable.message?.contains("Please Enter a valid Email") == true) {
                                    signupError = "Please Enter a valid Email Id"
                                    Toast.makeText(context, signupError, Toast.LENGTH_LONG).show()
                                }else if (throwable.message?.contains("Invalid password") == true) {
                                    signupError = "Invalid password"
                                    Toast.makeText(context, signupError, Toast.LENGTH_LONG).show()
                                }
                                else {
                                    signupError = "Signup failed: ${throwable.message ?: "Unknown error"}"
                                    Toast.makeText(context, signupError, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            signupError = "Exception: ${e.message}"
                            Toast.makeText(
                                context,
                                "Signup error: ${e.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.DarkGray, modifier = Modifier.height(24.dp))
                } else {
                    Text("CREATE ACCOUNT", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = {
                    navController.navigate("Login")
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.DarkGray
                )
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}