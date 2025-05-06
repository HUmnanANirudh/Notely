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
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true  // Add this for more lenient parsing
            coerceInputValues = true  // Helps with null handling
        })
    }
}

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    @SerialName("jwt")
    val jwt: String
)
suspend fun loginRequest(email: String, password: String): Result<String> {
    return try {
        // Log the request data for debugging
        println("Attempting login with email: $email")

        // Perform the POST request
        val response = client.post("https://likhlo.shukurenai123.workers.dev/api/v1/users/SignIn") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        // Get the response as text first for debugging
        val responseText = response.bodyAsText()
        println("Raw response status: ${response.status}")
        println("Raw response: $responseText")

        // Check if the response contains an error message
        if (responseText.contains("Invalid password") || responseText.contains("No such user exists")) {
            // This is a valid server response indicating authentication failed
            return Result.failure(Exception(
                if (responseText.contains("Invalid password")) "Invalid password"
                else "User not found"
            ))
        }

        // If we have a successful response, try to parse it
        try {
            val loginResponse: LoginResponse = Json.decodeFromString(responseText)
            Result.success(loginResponse.jwt)
        } catch (e: Exception) {
            // If standard deserialization fails, try manual parsing
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
        println("Login error: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}
@Composable
fun Login(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    text = "Welcome back",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Login to your account to continue sharing and creating notes.",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.White,
                cursorColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black,
                focusedTextColor = Color.Black,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
            Button(
                onClick = {
                    scope.launch {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        isLoading = true
                        loginError = ""

                        try {
                            val result = loginRequest(email, password)
                            result.onSuccess { jwt ->
                                val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                                sharedPrefs.edit().putString("jwt_token", jwt).apply()

                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                navController.navigate("notes")
                            }.onFailure { throwable ->
                                if (throwable.message?.contains("Invalid password") == true) {
                                    loginError = "Invalid password. Please check and try again."
                                    Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                                } else if (throwable.message?.contains("User not found") == true) {
                                    loginError = "User not found. Please check your email."
                                    Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                                } else {
                                    loginError = "Login failed: ${throwable.message ?: "Unknown error"}"
                                    Toast.makeText(context, loginError, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            loginError = "Exception: ${e.message}"
                            Toast.makeText(
                                context,
                                "Login error: ${e.message ?: "Unknown error"}",
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
            )
            {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.DarkGray, modifier = Modifier.height(24.dp))
                } else {
                    Text("LOGIN", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = {
                    navController.navigate("Signup")
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.DarkGray
                )
            ) {
                Text("Don't have an account? Sign up")
            }
        }
    }
}