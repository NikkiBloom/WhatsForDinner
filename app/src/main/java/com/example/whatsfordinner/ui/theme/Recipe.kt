package com.example.whatsfordinner.ui.theme

data class Recipe(
    val id: Int,
    val imageUri: String?,        // URI or file path
    val title: String,
    val tags: List<String>,
    val ingredients: List<String>,
    val instructions: String
)