package aritra.seal.food_analyzer.Recipe

// Data classes
data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val calories: Double? = null
)