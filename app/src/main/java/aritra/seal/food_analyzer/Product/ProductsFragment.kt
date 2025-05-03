package aritra.seal.food_analyzer.Product

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager

import aritra.seal.food_analyzer.databinding.FragmentProductsBinding
import com.google.firebase.database.*

class ProductsFragment : Fragment() {
    private lateinit var database: DatabaseReference
    private val categoryList = mutableListOf<String>()
    private val foodList = mutableListOf<FoodItem>()
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!  // Safe access to binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set RecyclerView properties
        binding.categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("categories")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (categorySnapshot in snapshot.children) {
                    categoryList.add(categorySnapshot.key ?: "")
                }
                binding.categoryRecyclerView.adapter = CategoryAdapter(categoryList) { category ->
                    val intent = Intent(requireContext(), FoodListActivity::class.java)
                    intent.putExtra("category", category)
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks
    }
}
