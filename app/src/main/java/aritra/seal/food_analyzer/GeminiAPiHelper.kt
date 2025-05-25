package aritra.seal.food_analyzer

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HealthyAlternative(
    val productName: String,
    val brand: String,
    val healthBenefits: String,
    val buyingLink: String
)

class GeminiApiHelper(private val apiKey: String) {
    private val model = GenerativeModel(modelName = "gemini-2.5-flash-preview-05-20", apiKey = apiKey, generationConfig = generationConfig {
        temperature = 0.1f // Try a low temperature for more deterministic results
        topK = 1
        topP = 0.9f
    })

    suspend fun analyzeIngredients(ingredients: String): Pair<String, Int?> {
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
                val resultText = response.text ?: return@withContext "Unable to analyze." to null

                // Parse the response
                val healthStatusRegex = Regex("""Healthy or Unhealthy:\s*(Healthy|Unhealthy)""")
                val unhealthyPercentRegex = Regex("""Unhealthy Percentage:\s*(\d+)%""")

                val healthMatch = healthStatusRegex.find(resultText)
                val percentMatch = unhealthyPercentRegex.find(resultText)

                val healthStatus = healthMatch?.groupValues?.get(1) ?: "Unknown"
                val percent = percentMatch?.groupValues?.get(1)?.toIntOrNull()

                return@withContext healthStatus to percent

            } catch (e: Exception) {
                "Error" to null
            }
        }
    }

    suspend fun getHealthyAlternatives(productCategory: String): List<HealthyAlternative> {
        return withContext(Dispatchers.IO) {
            try {
                // --- MODIFIED PROMPT BELOW ---
                val prompt = """
                Provide 5-7 healthy alternatives for the following product category: $productCategory.
                **Crucially, all products must be widely available and easily purchasable in India.**
                
                For each alternative, provide:
                1. Product name
                2. Brand name
                3. Health benefits (brief description)
                4. A generic buying link (preferably to Indian e-commerce sites like Amazon India, BigBasket, Zepto, Flipkart or the brand's official Indian website).
                
                Format your response exactly like this, with no extra introductory or concluding remarks:
                
                PRODUCT: [Product Name]
                BRAND: [Brand Name]
                BENEFITS: [Health benefits description]
                LINK: [Buying link - use Indian specific links like https://www.amazon.in/s?k=product+name or bigbasket.com or brand websites available in India]
                ---
                
                Make sure to suggest real, widely available products with accurate information for the Indian market.
                Focus on healthier alternatives that are genuinely better options.

                Example for "White Bread":
                PRODUCT: 100% Whole Wheat Bread
                BRAND: Britannia
                BENEFITS: Made with whole grains, rich in fiber, aids digestion, no maida.
                LINK: https://www.bigbasket.com/pd/40162924/britannia-100-whole-wheat-bread-450-g-pouch/
                ---
                PRODUCT: Multigrain Bread
                BRAND: Harvest Gold
                BENEFITS: Contains multiple grains and seeds, high in fiber, supports heart health.
                LINK: https://www.zeptonow.com/pn/harvest-gold-multigrain-bread-13-grains-seeds-zero-maida/pvid/8dd16b08-eb6d-446f-a5bc-545cefc17171
                ---
                PRODUCT: Multigrain Sourdough Bread
                BRAND: The Baker's Dozen
                BENEFITS: Fermented for better digestion, lower glycemic index, no preservatives.
                LINK: https://www.amazon.in/Bakers-Dozen-Multigrain-Loaf-220g/dp/B07RDPN47K
                ---
            """.trimIndent()
                // --- END MODIFIED PROMPT ---

                val response = model.generateContent(prompt)
                val resultText = response.text

                // Log the raw response for debugging (keep this!)
                Log.d("GeminiApiHelper", "Raw API Response for '$productCategory' (India-specific):\n$resultText")

                if (resultText.isNullOrEmpty()) {
                    Log.e("GeminiApiHelper", "Gemini API returned null or empty response for India-specific alternatives.")
                    return@withContext emptyList()
                }

                return@withContext parseHealthyAlternatives(resultText)

            } catch (e: Exception) {
                Log.e("GeminiApiHelper", "Error fetching India-specific alternatives: ${e.message}", e)
                emptyList()
            }
        }
    }

    private fun parseHealthyAlternatives(responseText: String): List<HealthyAlternative> {
        val alternatives = mutableListOf<HealthyAlternative>()

        // Log before parsing
        Log.d("GeminiApiHelper", "Parsing response:\n$responseText")

        // Split by "---" to get individual product entries
        val productEntries = responseText.split("---")

        for (entry in productEntries) {
            if (entry.trim().isEmpty()) continue

            val lines = entry.trim().split("\n")
            var productName = ""
            var brand = ""
            var benefits = ""
            var link = ""

            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("PRODUCT:", ignoreCase = true) -> {
                        productName = trimmedLine.substringAfter("PRODUCT:", "").trim()
                    }
                    trimmedLine.startsWith("BRAND:", ignoreCase = true) -> {
                        brand = trimmedLine.substringAfter("BRAND:", "").trim()
                    }
                    trimmedLine.startsWith("BENEFITS:", ignoreCase = true) -> {
                        benefits = trimmedLine.substringAfter("BENEFITS:", "").trim()
                    }
                    trimmedLine.startsWith("LINK:", ignoreCase = true) -> {
                        link = trimmedLine.substringAfter("LINK:", "").trim()
                    }
                }
            }

            // Only add if we have at least product name and some other info
            // And ensure the link is a valid URL to avoid malformed intents
            if (productName.isNotEmpty() && (brand.isNotEmpty() || benefits.isNotEmpty())) {
                // If no specific link provided or if it's not a valid URL, create a generic Amazon India search link
                if (link.isEmpty() || !link.startsWith("http")) {
                    val searchQuery = "$brand $productName".replace(" ", "+")
                    link = "https://www.amazon.in/s?k=$searchQuery"
                }

                alternatives.add(
                    HealthyAlternative(
                        productName = productName,
                        brand = brand,
                        healthBenefits = benefits,
                        buyingLink = link
                    )
                )
            }
        }

        return alternatives
    }
}