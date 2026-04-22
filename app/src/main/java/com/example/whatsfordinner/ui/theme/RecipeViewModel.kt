package com.example.whatsfordinner.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(private val dao: RecipeDAO) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // searches INGREDIENTS
    private val _iHaveIngredients = MutableStateFlow<List<String>>(emptyList())
    val iHaveIngredients: StateFlow<List<String>> = _iHaveIngredients

    // searches TAGS
    private val _iHaveCravings = MutableStateFlow<List<String>>(emptyList())
    val iWantTags: StateFlow<List<String>> = _iHaveCravings

    // Filtered recipes based on search, ingredients, or cravings
    val filteredRecipes: StateFlow<List<RecipeTuple>> = combine(
        _searchQuery,
        _iHaveIngredients,
        _iHaveCravings
    ) { query, iHave, iCrave ->
        Triple(query, iHave, iCrave)
    }.flatMapLatest { (query, iHave, iCrave) ->
        if (iHave.isNotEmpty() || iCrave.isNotEmpty()) {
            dao.getAllRecipesFlow().map { allRecipes: List<RecipeTuple> ->
                allRecipes.mapNotNull { recipe ->
                    var matchCount = 0

                    if (iHave.isNotEmpty()) {
                        val recipeIngredients = recipe.ingredients ?: emptyList()
                        matchCount += iHave.count { have ->
                            val cleanHave = have.trim()
                            recipeIngredients.any { it.contains(cleanHave, ignoreCase = true) }
                        }
                    }

                    if (iCrave.isNotEmpty()) {
                        val recipeTags = recipe.tags ?: emptyList()
                        matchCount += iCrave.count { crave ->
                            val cleanCrave = crave.trim()
                            recipeTags.any { it.contains(cleanCrave, ignoreCase = true) }
                        }
                    }

                    if (matchCount > 0) {
                        recipe to matchCount

                    } else {
                        null
                    }
                }
                .sortedByDescending { it.second }
                .map { it.first }
            }
        } else if (query.isBlank()) {
            dao.getAllRecipesFlow()
        } else {
            dao.searchIngredientsFlow(query)
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setIHaveIngredients(ingredients: List<String>) {
        _iHaveIngredients.value = ingredients
        _searchQuery.value = ""
    }

    fun setIHaveCravings(tags: List<String>) {
        _iHaveCravings.value = tags
        _searchQuery.value = ""
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _iHaveIngredients.value = emptyList()
    }

    fun addRecipe(
        title: String,
        credit: String?,
        imageUri: String?,
        tags: List<String>,
        ingredients: List<String>,
        instructions: String
    ) {
        viewModelScope.launch {
            val recipe = Recipe(
                id = 0, // FTS rowid will be auto-generated
                title = title,
                credit = credit,
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