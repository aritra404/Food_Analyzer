package aritra.seal.food_analyzer.Product

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import aritra.seal.food_analyzer.databinding.ActivityFoodDetailBinding
import com.bumptech.glide.Glide

class FoodDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get food details from Intent
        val foodTitle = intent.getStringExtra("title") ?: "Unknown"
        val foodImage = intent.getStringExtra("image") ?: ""
        val foodRating = intent.getStringExtra("rating") ?: "N/A"
        val foodPrice = intent.getStringExtra("price") ?: "N/A"
        val foodCalories = intent.getStringExtra("calories") ?: "N/A"
        val foodProtein = intent.getStringExtra("protein") ?: "N/A"
        val foodCarbs = intent.getStringExtra("carbs") ?: "N/A"
        val foodFats = intent.getStringExtra("fats") ?: "N/A"
        val buyLink = intent.getStringExtra("buyLink") ?: ""

        // Debugging: Print received data
        println("Title: $foodTitle, Price: $foodPrice, Image: $foodImage")

        // Set data to UI
        binding.foodTitle.text = foodTitle
        binding.foodRating.text = "⭐ $foodRating"
        binding.foodPrice.text = foodPrice
        binding.calories.text = "Calories: $foodCalories kcal"
        binding.protein.text = "Protein: $foodProtein g"
        binding.carbs.text = "Carbs: $foodCarbs g"
        binding.fats.text = "Fats: $foodFats g"

        // Load image using Glide
        Glide.with(this).load(foodImage).into(binding.foodImage)

        // Buy Now button click
        binding.buyNowButton.setOnClickListener {
            if (buyLink.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buyLink))
                startActivity(intent)
            }
        }

        // Back button click
        binding.backButton.setOnClickListener {
            finish()
        }
    }
}


