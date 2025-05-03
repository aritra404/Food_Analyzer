package aritra.seal.food_analyzer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import aritra.seal.food_analyzer.Product.ProductsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProductsFragment()
            1 -> RecipesFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}
