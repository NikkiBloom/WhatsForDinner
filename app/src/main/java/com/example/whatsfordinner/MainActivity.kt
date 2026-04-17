package com.example.whatsfordinner

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import coil.compose.rememberAsyncImagePainter
import com.example.whatsfordinner.ui.theme.Recipe
import com.example.whatsfordinner.ui.theme.RecipeDAO
import com.example.whatsfordinner.ui.theme.RecipeDatabase
import com.example.whatsfordinner.ui.theme.RecipeTuple
import com.example.whatsfordinner.ui.theme.RecipeViewModel
import com.example.whatsfordinner.ui.theme.WhatsForDinnerTheme
import org.json.JSONArray
import java.nio.charset.Charset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide home bar except on swipe
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val db = Room.databaseBuilder(
            applicationContext,
            RecipeDatabase::class.java, "recipe-database"

        // pre-load from json file:
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                try {
                    val inputStream = applicationContext.assets.open("preloadRecipes.json")
                    val size = inputStream.available()
                    val buffer = ByteArray(size)
                    inputStream.read(buffer)
                    inputStream.close()
                    val jsonString = String(buffer, Charset.forName("UTF-8"))
                    val jsonArray = JSONArray(jsonString)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val title = obj.getString("title")
                        val tags = obj.getJSONArray("tags").let { arr ->
                            List(arr.length()) { arr.getString(it) }.joinToString(",")
                        }
                        val ingredients = obj.getJSONArray("ingredients").let { arr ->
                            List(arr.length()) { arr.getString(it) }.joinToString(",")
                        }
                        val instructions = obj.getString("instructions")

                        db.execSQL(
                            "INSERT INTO RecipeDatabase (title, tags, ingredients, instructions) VALUES (?, ?, ?, ?)",
                            arrayOf(title, tags, ingredients, instructions)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }).build()

        val dao = db.recipeDAO()
        val viewModelFactory = RecipeViewModelFactory(dao)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WhatsForDinnerTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val recipeViewModel: RecipeViewModel = viewModel(factory = viewModelFactory)

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            navController = navController,
                            recipeViewModel = recipeViewModel,
                        )
                    }
                    composable("recipes") {
                        RecipeBook(navController, recipeViewModel = recipeViewModel)
                    }
                    composable("newRecipe") {
                        NewRecipeScreen(navController, recipeViewModel = recipeViewModel)
                    }

                    composable("have"){
                        IHaveScreen(navController = navController, recipeViewModel = recipeViewModel)
                    }

                    composable("crave"){
                        ICraveScreen(navController = navController, recipeViewModel = recipeViewModel)
                    }

                    composable(
                        route = "fullRecipe/{recipeId}",
                        arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments!!.getInt("recipeId")
                        FullRecipeView(
                            navController = navController,
                            recipeId = recipeId,
                            recipeViewModel = recipeViewModel
                        )
                    }

                    composable(
                        route = "editRecipe/{recipeId}",
                        arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments!!.getInt("recipeId")
                        EditRecipeScreen(
                            navController = navController,
                            recipeId = recipeId,
                            recipeViewModel = recipeViewModel
                        )
                    }

                    composable("recipeTinder") {
                        RecipeTinderScreen(recipeViewModel, navController)
                    }

                }
            }
        }
    }
}

// needed to connect view model and DAO
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
fun MainScreen(navController: NavController, recipeViewModel: RecipeViewModel, modifier: Modifier = Modifier) {
    // clear search queries when on main screen
    recipeViewModel.setIHaveIngredients(emptyList())
    recipeViewModel.setIHaveCravings(emptyList())

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize(),
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
                onClick = { navController.navigate("crave") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("I'm Craving...")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { navController.navigate("have") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("I Have...")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    recipeViewModel.clearSearch()
                    navController.navigate("recipeTinder")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("I Don't Know (Random)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate("recipes") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text("Recipe Book")
            }
        }
    }
}

// "short display" of recipes for recipe book screen
@Composable
fun RecipeCard(recipe: RecipeTuple, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable{ onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)) {

            recipe.imageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .width(90.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(modifier = Modifier.padding(16.dp)) {

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
}

// navigable screen to view all recipes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBook(
    navController: NavController,
    recipeViewModel: RecipeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {

    val recipes by recipeViewModel.filteredRecipes.collectAsState()
    val searchQuery by recipeViewModel.searchQuery.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
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

            //todo
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
                    RecipeCard(recipe) {
                        navController.navigate("editRecipe/${recipe.id}")
                    }
                }
            }
        }

        // back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                color = MaterialTheme.colorScheme.secondary
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
    var tags by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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

            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Recipe Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // image select button
            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Pick Image")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredients (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Flavor Profile (comma-separated)") },
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
                onClick = { // todo
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
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save Recipe")
            }
        }

        // back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

// navigable screen to edit a recipe
@Composable
fun EditRecipeScreen(
    navController: NavController,
    recipeId: Int,
    recipeViewModel: RecipeViewModel
) {
    val recipe by recipeViewModel
        .getRecipe(recipeId)
        .collectAsState(initial = null)

    recipe?.let { existing ->

        var title by remember { mutableStateOf(existing.title) }
        var ingredients by remember { mutableStateOf(existing.ingredients.joinToString(", ")) }
        var tags by remember { mutableStateOf(existing.tags.joinToString(", ")) }
        var instructions by remember { mutableStateOf(existing.instructions) }
        var imageUri by remember { mutableStateOf(existing.imageUri?.let { Uri.parse(it) }) }

        val imagePicker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            imageUri = uri
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Edit Recipe",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Recipe Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB16565))
            ) {
                Text("Change Image")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(title, { title = it }, label = { Text("Title") })

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                ingredients,
                { ingredients = it },
                label = { Text("Ingredients (comma-separated)") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                tags,
                { tags = it },
                label = { Text("Flavor Profile (comma-separated)") },
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                instructions,
                { instructions = it },
                label = { Text("Instructions") },
                minLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    recipeViewModel.updateRecipe(
                        existing.copy(
                            title = title,
                            imageUri = imageUri?.toString(),
                            ingredients = ingredients
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() },
                            tags = tags
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() },
                            instructions = instructions
                        )
                    )
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Changes")
            }
        }
    }
}

// navigable screen to view a full recipe
@Composable
fun FullRecipeView(
    navController: NavController,
    recipeId: Int,
    recipeViewModel: RecipeViewModel
) {
    val recipe by recipeViewModel
        .getRecipe(recipeId)
        .collectAsState(initial = null)

    recipe?.let { existing ->

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(12.dp)
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                existing.imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(uri)),
                        contentDescription = existing.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = existing.title,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = existing.ingredients.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Flavor Profile",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = existing.tags.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = existing.instructions,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Space so content doesn't hide behind button
                Spacer(modifier = Modifier.height(80.dp))
            }

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text("Back")
            }
        }
    }
}

// recipe cards for tinder screen
@Composable
fun TinderCard(recipe: RecipeTuple) {
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
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 25.sp
            )
        }
    }
}

@Composable
fun TinderButtons(
    onReject: () -> Unit,
    onAccept: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        IconButton(
            onClick = onReject,
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFFE0E0), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Skip")
        }

        IconButton(
            onClick = onAccept,
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE0F2F1), CircleShape)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Open")
        }
    }
}

@Composable
fun RecipeTinderScreen(
    recipeViewModel: RecipeViewModel,
    navController: NavController
) {
    val recipes by recipeViewModel.filteredRecipes.collectAsState()
    val iHaveIngredients by recipeViewModel.iHaveIngredients.collectAsState()
    
    var displayRecipes by remember { mutableStateOf<List<RecipeTuple>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }

    // Update displayRecipes when recipes change
    LaunchedEffect(recipes) {
        if (iHaveIngredients.isNotEmpty()) {
            // Respect the order from ViewModel (sorted by match count)
            displayRecipes = recipes
        } else {
            // Randomize if not an "I Have" search
            displayRecipes = recipes.shuffled()
        }
        currentIndex = 0
    }

    val currentRecipe = displayRecipes.getOrNull(currentIndex)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (currentRecipe == null) {
            Text(
                text = "Out of recipes! Try a new search.",
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TinderCard(
                    recipe = currentRecipe
                )

                Spacer(modifier = Modifier.height(24.dp))

                TinderButtons(
                    onReject = { currentIndex++ },
                    onAccept = {
                        navController.navigate("fullRecipe/${currentRecipe.id}")
                    }
                )
            }
        }

        // Back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun IHaveScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    navController: NavController,
){
    var ingredients by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "I Have...",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredients (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    val list = ingredients.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    recipeViewModel.setIHaveIngredients(list)
                    navController.navigate("recipeTinder")
                          },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text("Find My Dinner")
            }
        }

            // back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun ICraveScreen(
    recipeViewModel: RecipeViewModel,
    navController: NavController){
    var tags by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "I'm Craving...",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Cravings (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val list = tags.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    recipeViewModel.setIHaveCravings(list)
                    navController.navigate("recipeTinder")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text("Find My Dinner")
            }
        }

        // back button
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}