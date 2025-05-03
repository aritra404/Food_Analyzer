package aritra.seal.food_analyzer.Recipe

import retrofit2.http.GET
import retrofit2.http.Query

class ApiInterface {

    interface SpoonacularApiService {
        @GET("recipes/findByNutrients")
        suspend fun getRecipesByNutrients(
            @Query("apiKey") apiKey: String,
            @Query("maxCalories") maxCalories: Int
        ): List<Recipe>

        @GET("recipes/findByIngredients")
        suspend fun getRecipesByIngredients(
            @Query("apiKey") apiKey: String,
            @Query("ingredients") ingredients: String
        ): List<Recipe>
    }

}