package com.example.whatsfordinner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import coil.compose.rememberAsyncImagePainter
import com.example.whatsfordinner.ui.theme.Recipe
import com.example.whatsfordinner.ui.theme.RecipeDAO
import com.example.whatsfordinner.ui.theme.RecipeDatabase
import com.example.whatsfordinner.ui.theme.RecipeTuple
import com.example.whatsfordinner.ui.theme.RecipeViewModel
import com.example.whatsfordinner.ui.theme.WhatsForDinnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            RecipeDatabase::class.java, "recipe-database"
        ).build()
        val dao = db.RecipeDAO()
        val viewModelFactory = RecipeViewModelFactory(dao)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WhatsForDinnerTheme {
                val navController = rememberNavController()
                val recipeViewModel: RecipeViewModel = viewModel(factory = viewModelFactory)

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            MainScreen(navController = navController, modifier = Modifier.padding(innerPadding))
                        }
                    }
                    composable("recipes") {
                        RecipeBook(navController, recipeViewModel = recipeViewModel)
                    }
                    composable("newRecipe") {
                        NewRecipeScreen(navController, recipeViewModel = recipeViewModel)
                    }
                }
            }
        }
    }
}

class RecipeViewModelFactory(private val dao: RecipeDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainScreen(navController: NavController, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8EBEB))
    ) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            //.background(Color(0xFFF8EBEB)),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "What's For Dinner?",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(150.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB16565))
            ) {
                Text("I'm Craving...")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB16565))
            ) {
                Text("I Have...")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB16565))
            ) {
                Text("I Don't Know (Random)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate("recipes") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB16565)),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text("Recipe Book")
            }
        }
    }
}

// "short display" of recipes for recipe book screen
@Composable
fun RecipeCard(recipe: RecipeTuple) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            recipe.imageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium
            )

            if (recipe.tags?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recipe.tags.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            recipe.ingredients?.let {
                Text(
                    text = it.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// navigable screen to view all recipes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBook(
    navController: NavController,
    recipeViewModel: RecipeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    //todo?
    val recipes by recipeViewModel.filteredRecipes.collectAsState()
    val searchQuery by recipeViewModel.searchQuery.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8EBEB))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recipe Book",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = recipeViewModel::onSearchQueryChange,
                label = { Text("Search Recipe Cards") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    NewRecipeCard(
                        onClick = { navController.navigate("newRecipe") },
                    )
                }
                items(recipes) { recipe ->
                    RecipeCard(recipe)
                }
            }
        }

        // back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB16565)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

// special recipe card to open "new recipe" screen
@Composable
fun NewRecipeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE6CFCF)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ New Recipe",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF6A2E2E)
            )
        }
    }
}

// navigable screen to enter a new recipe
@Composable
fun NewRecipeScreen(
    navController: NavController,
    recipeViewModel: RecipeViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8EBEB))
            .systemBarsPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "New Recipe",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredients (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    recipeViewModel.addRecipe(
                        title = title,
                        imageUri = null,
                        tags = emptyList(),
                        ingredients = ingredients
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() },
                        instructions = instructions
                    )
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB16565)
                )
            ) {
                Text("Save Recipe")
            }
        }

        // back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB16565)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}