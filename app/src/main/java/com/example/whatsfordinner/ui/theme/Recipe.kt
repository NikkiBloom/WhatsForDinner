package com.example.whatsfordinner.ui.theme
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey @ColumnInfo(name = "rowid") val id: Int,
    val imageUri: String?,        // URI or file path
    val title: String,
    val credit: String?,
    val tags: List<String>,
    val ingredients: List<String>,
    val instructions: String
)

data class RecipeTuple (
    val id: Int,
    val imageUri: String?,
    val title: String,
    val credit: String?,
    val tags: List<String>?,
    val ingredients: List<String>?,
    val instructions: String?
)