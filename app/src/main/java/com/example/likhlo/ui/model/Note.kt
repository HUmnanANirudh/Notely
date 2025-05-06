package com.example.likhlo.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String? = null,
    val title: String,
    val content: String
)