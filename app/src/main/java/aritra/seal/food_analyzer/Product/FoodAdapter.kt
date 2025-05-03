package aritra.seal.food_analyzer.Product

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import aritra.seal.food_analyzer.R
import com.bumptech.glide.Glide

class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.foodTitle)
        val image: ImageView = view.findViewById(R.id.foodImage)
        val rating: TextView = view.findViewById(R.id.foodRating)
        val price: TextView = view.findViewById(R.id.foodPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        holder.title.text = food.title
        holder.rating.text = "⭐ ${food.rating}"
        holder.price.text = food.price
        Glide.with(holder.image.context).load(food.image).into(holder.image)

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(food)
        }
    }

    override fun getItemCount(): Int = foodList.size
}

