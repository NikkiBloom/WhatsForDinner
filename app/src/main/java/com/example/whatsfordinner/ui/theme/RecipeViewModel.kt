package com.example.whatsfordinner.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RecipeViewModel : ViewModel() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    private var nextId = 0

    val searchQuery: StateFlow<String> = _searchQuery

    // Filtered recipes based on ingredient search
    val filteredRecipes: StateFlow<List<Recipe>> =
        combine(_recipes, _searchQuery) { recipes, query ->
            if (query.isBlank()) {
                recipes
            } else {
                recipes.filter { recipe ->
                    recipe.ingredients.any {
                        it.contains(query, ignoreCase = true)
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecipe(
        title: String,
        imageUri: String?,
        tags: List<String>,
        ingredients: List<String>,
        instructions: String
    ) {
        val recipe = Recipe(
            id = nextId++,
            title = title,
            imageUri = imageUri,
            tags = tags,
            ingredients = ingredients,
            instructions = instructions
        )
        _recipes.value = _recipes.value + recipe
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}