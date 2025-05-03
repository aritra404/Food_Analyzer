package aritra.seal.food_analyzer

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiApiHelper(private val apiKey: String) {
    private val model = GenerativeModel(modelName = "gemini-pro", apiKey = apiKey)

    suspend fun analyzeIngredients(ingredients: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze the following food ingredients and determine if the product is healthy or unhealthy.
                    If unhealthy, provide a percentage for how unhealthy it is:
                    
                    Ingredients: $ingredients
                    
                    Response format: 
                    Healthy or Unhealthy: [Healthy/Unhealthy]
                    Unhealthy Percentage: [xx%]
                """.trimIndent()

                val response = model.generateContent(prompt)
                response.text ?: "Unable to analyze ingredients."
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}
