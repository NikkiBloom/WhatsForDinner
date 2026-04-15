package com.example.whatsfordinner.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(private val dao: RecipeDAO) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Filtered recipes based on ingredient search from Room
    val filteredRecipes: StateFlow<List<RecipeTuple>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                dao.getAllRecipesFlow()
            } else {
                dao.searchIngredientsFlow("%$query%")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecipe(
        title: String,
        imageUri: String?,
        tags: List<String>,
        ingredients: List<String>,
        instructions: String
    ) {
        viewModelScope.launch {
            val recipe = Recipe(
                id = 0, // FTS rowid will be auto-generated
                title = title,
                imageUri = imageUri,
                tags = tags,
                ingredients = ingredients,
                instructions = instructions
            )
            dao.insertRecipe(recipe)
        }
    }

    fun getRecipe(recipeId: Int): Flow<Recipe?> =
        flow {
            emit(dao.getRecipeById(recipeId))
        }

    fun updateRecipe(recipe: Recipe) {
        viewModelScope.launch {
            dao.updateRecipe(recipe)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}