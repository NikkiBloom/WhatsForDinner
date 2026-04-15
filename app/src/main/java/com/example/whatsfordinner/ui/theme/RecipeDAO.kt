package com.example.whatsfordinner.ui.theme

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(vararg recipe: Recipe)

    @Update
    suspend fun updateRecipe(vararg recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("SELECT rowid AS id, * FROM RecipeDatabase")
    fun getAllRecipesFlow(): Flow<List<RecipeTuple>>

    @Query("SELECT rowid AS id, * FROM RecipeDatabase WHERE ingredients LIKE :query")
    fun searchIngredientsFlow(query: String): Flow<List<RecipeTuple>>

    @Query("SELECT rowid AS id, * FROM RecipeDatabase")
    fun getBook(): List<RecipeTuple>
}