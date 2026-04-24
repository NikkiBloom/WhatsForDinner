package com.example.whatsfordinner.ui.theme

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// actual database
@Database(entities = [Recipe::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDAO(): RecipeDAO
}