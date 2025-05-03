package aritra.seal.food_analyzer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import aritra.seal.food_analyzer.Recipe.ApiInterface
import aritra.seal.food_analyzer.Recipe.RecipesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipesFragment : Fragment() {

    private lateinit var calorieInput: EditText
    private lateinit var ingredientInput: EditText
    private lateinit var findRecipesButton: Button
    private lateinit var recipeRecyclerView: RecyclerView
    private val recipesAdapter = RecipesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_recipes, container, false)

        // Initialize views
        calorieInput = view.findViewById(R.id.calorieInput)
        ingredientInput = view.findViewById(R.id.ingredientInput)
        findRecipesButton = view.findViewById(R.id.findRecipesButton)
        recipeRecyclerView = view.findViewById(R.id.recipeRecyclerView)

        // Setup RecyclerView
        recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recipeRecyclerView.adapter = recipesAdapter

        // Set up Retrofit
        val apiKey = "9a457bcdff334a93a56744b18a8114b4" // Replace with your Spoonacular API key

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Logging interceptor for debugging
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url

                val newUrl = originalUrl.newBuilder()
                    .addQueryParameter("apiKey", apiKey) // Append API key as a query parameter
                    .build()

                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .build()

                chain.proceed(newRequest)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .client(client) // Pass the custom OkHttpClient
            .addConverterFactory(GsonConverterFactory.create())
            .build()


        val apiService = retrofit.create(ApiInterface.SpoonacularApiService::class.java)

        // Set click listener for the button
        findRecipesButton.setOnClickListener {
            val calorieText = calorieInput.text.toString()
            val ingredientText = ingredientInput.text.toString()

            Log.d(
                "RecipesFragment",
                "Button clicked, calorie: $calorieText, ingredient: $ingredientText"
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(
                        "RecipesFragment",
                        "Calling API with calorie: $calorieText, ingredient: $ingredientText"
                    )

                    val results = if (calorieText.isNotEmpty()) {
                        apiService.getRecipesByNutrients(apiKey, calorieText.toInt())
                    } else {
                        apiService.getRecipesByIngredients(apiKey, ingredientText)
                    }

                    Log.d("RecipesFragment", "API call successful: ${results.size} recipes found")

                    // Update RecyclerView on the main thread
                    withContext(Dispatchers.Main) {
                        recipesAdapter.updateRecipes(results)
                    }
                } catch (e: Exception) {
                    Log.e("RecipesFragment", "API call failed", e)
                    // Handle API errors, e.g., show a toast (on the main thread)
                    withContext(Dispatchers.Main) {
                        // Show error message here, e.g., Toast
                    }
                }
            }

        }
return view
    }
}