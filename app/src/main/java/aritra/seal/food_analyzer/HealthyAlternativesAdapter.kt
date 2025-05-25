package aritra.seal.food_analyzer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HealthyAlternativesAdapter(
    private val onBuyClickListener: (HealthyAlternative) -> Unit
) : RecyclerView.Adapter<HealthyAlternativesAdapter.AlternativeViewHolder>() {

    private var alternatives = listOf<HealthyAlternative>()

    fun updateAlternatives(newAlternatives: List<HealthyAlternative>) {
        alternatives = newAlternatives
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlternativeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_healthy_alternative, parent, false)
        return AlternativeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlternativeViewHolder, position: Int) {
        holder.bind(alternatives[position])
    }

    override fun getItemCount(): Int = alternatives.size

    inner class AlternativeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameText: TextView = itemView.findViewById(R.id.productNameText)
        private val brandText: TextView = itemView.findViewById(R.id.brandText)
        private val benefitsText: TextView = itemView.findViewById(R.id.benefitsText)
        private val buyButton: Button = itemView.findViewById(R.id.buyButton)

        fun bind(alternative: HealthyAlternative) {
            productNameText.text = alternative.productName

            if (alternative.brand.isNotEmpty()) {
                brandText.text = "Brand: ${alternative.brand}"
                brandText.visibility = View.VISIBLE
            } else {
                brandText.visibility = View.GONE
            }

            if (alternative.healthBenefits.isNotEmpty()) {
                benefitsText.text = alternative.healthBenefits
                benefitsText.visibility = View.VISIBLE
            } else {
                benefitsText.visibility = View.GONE
            }

            buyButton.setOnClickListener {
                onBuyClickListener(alternative)
            }
        }
    }
}