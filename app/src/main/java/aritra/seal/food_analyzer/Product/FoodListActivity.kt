package aritra.seal.food_analyzer.Product

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import aritra.seal.food_analyzer.databinding.ActivityFoodListBinding
import com.google.firebase.database.*

class FoodListActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private val foodList = mutableListOf<FoodItem>()


    private lateinit var binding : ActivityFoodListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category = intent.getStringExtra("category") ?: return
       binding.foodRecyclerView.layoutManager = GridLayoutManager(this,2)

        database = FirebaseDatabase.getInstance().getReference("categories/$category")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                for (foodSnapshot in snapshot.children) {
                    val food = foodSnapshot.getValue(FoodItem::class.java)
                    if (food != null) foodList.add(food)
                }
                binding.foodRecyclerView.adapter = FoodAdapter(foodList) { foodItem ->
                    val intent = Intent(this@FoodListActivity, FoodDetailActivity::class.java).apply {
                        putExtra("title", foodItem.title)
                        putExtra("image", foodItem.image)
                        putExtra("rating", foodItem.rating)
                        putExtra("price", foodItem.price)
                        putExtra("calories", foodItem.calories)
                        putExtra("protein", foodItem.protein)
                        putExtra("carbs", foodItem.carbs)
                        putExtra("fats", foodItem.fats)
                        putExtra("buyLink", foodItem.buyLink)
                    }
                    startActivity(intent)
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }
}
