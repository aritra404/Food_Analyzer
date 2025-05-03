package aritra.seal.food_analyzer.Recipe

import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import aritra.seal.food_analyzer.R
import com.bumptech.glide.Glide

class RecipesAdapter : RecyclerView.Adapter<RecipesAdapter.RecipeViewHolder>() {
    private var recipes: List<Recipe> = emptyList()

    fun updateRecipes(newRecipes: List<Recipe>) {
        Log.d("RecipesAdapter", "Updating recipes, new list size: ${newRecipes.size}")
        recipes = newRecipes
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: android.view.ViewGroup,
        viewType: Int
    ): RecipeViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.recipe_item, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)
    }

    override fun getItemCount(): Int = recipes.size

    class RecipeViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: android.widget.ImageView = itemView.findViewById(R.id.recipeImage)
        private val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        private val recipeCalories: TextView = itemView.findViewById(R.id.recipeCalories)

        fun bind(recipe: Recipe) {
            // Load the image using Glide
            Glide.with(itemView.context)
                .load(recipe.image) // URL of the image
                .into(recipeImage)

            // Set the title and calories
            recipeTitle.text = recipe.title
            recipe.calories?.let {
                recipeCalories.text = "Calories: ${it.toInt()}"
            }
        }
    }

}
