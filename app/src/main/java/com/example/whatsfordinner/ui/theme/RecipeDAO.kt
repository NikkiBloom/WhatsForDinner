package com.example.whatsfordinner.ui.theme

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// data access object for database
@Dao
interface RecipeDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(vararg recipe: Recipe)

    @Update
    suspend fun updateRecipe(vararg recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("SELECT rowid, * FROM recipes WHERE rowid = :id")
    suspend fun getRecipeById(id: Int): Recipe

    @Query("SELECT rowid AS id, * FROM recipes")
    fun getAllRecipesFlow(): Flow<List<RecipeTuple>>

    @Query("SELECT rowid AS id, * FROM recipes WHERE recipes MATCH :query")
    fun searchIngredientsFlow(query: String): Flow<List<RecipeTuple>>

    @Query("SELECT rowid AS id, * FROM recipes")
    fun getBook(): List<RecipeTuple>
}