package aritra.seal.food_analyzer

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var healthyPercentage: TextView
    private lateinit var unhealthyPercentage: TextView
    private lateinit var analysisResult: TextView
    private lateinit var ingredientsList: TextView
    private lateinit var backButton: ImageView
    private lateinit var scanAgainButton: Button
    private lateinit var saveResultButton: Button

    // New views for healthy alternatives
    private lateinit var unhealthyWarning: LinearLayout
    private lateinit var productSearchLayout: LinearLayout
    private lateinit var productSearchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var alternativesRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var geminiHelper: GeminiApiHelper
    private lateinit var alternativesAdapter: HealthyAlternativesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Initialize Gemini helper (you'll need to provide your API key)
        geminiHelper = GeminiApiHelper("AIzaSyBTPSWqcv1bPsghiM4gRttVL3xYhFvqB84")

        initializeViews()
        setupChart()
        displayResults()
        setupClickListeners()
        setupAlternativesRecyclerView()
    }

    private fun initializeViews() {
        pieChart = findViewById(R.id.pieChart)
        healthyPercentage = findViewById(R.id.healthyPercentage)
        unhealthyPercentage = findViewById(R.id.unhealthyPercentage)
        analysisResult = findViewById(R.id.analysisResult)
        ingredientsList = findViewById(R.id.ingredientsList)
        backButton = findViewById(R.id.backButton)
        scanAgainButton = findViewById(R.id.scanAgainButton)
        saveResultButton = findViewById(R.id.saveResultButton)

        // New views
        unhealthyWarning = findViewById(R.id.unhealthyWarning)
        productSearchLayout = findViewById(R.id.productSearchLayout)
        productSearchInput = findViewById(R.id.productSearchInput)
        searchButton = findViewById(R.id.searchButton)
        alternativesRecyclerView = findViewById(R.id.alternativesRecyclerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
    }

    private fun setupChart() {
        // Configure chart appearance
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f

            // Enable hole and configure
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f

            // Center text
            setDrawCenterText(true)
            centerText = "Health\nScore"
            setCenterTextSize(16f)
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)
            setCenterTextColor(Color.parseColor("#1F2937"))

            // Rotation
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // Legend
            legend.isEnabled = false

            // Entry label styling
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    private fun displayResults() {
        // Get data from intent with proper extra keys
        val healthStatus = intent.getStringExtra(EXTRA_HEALTH_STATUS) ?: "Unknown"
        val unhealthyPercent = intent.getIntExtra(EXTRA_UNHEALTHY_PERCENT, 0)
        val ingredients = intent.getStringExtra(EXTRA_INGREDIENTS) ?: "No ingredients provided"

        // Calculate healthy percentage
        val healthyPercent = 100 - unhealthyPercent

        // Update percentage displays
        healthyPercentage.text = "${healthyPercent}%"
        unhealthyPercentage.text = "${unhealthyPercent}%"

        // Display analysis result and ingredients
        val resultText = buildString {
            append("Overall Status: $healthStatus\n")
            append("Healthy: ${healthyPercent}%\n")
            append("Unhealthy: ${unhealthyPercent}%")
        }
        analysisResult.text = resultText
        ingredientsList.text = ingredients

        // Show healthy alternatives section if product is unhealthy above 25%
        if (unhealthyPercent > 25) {
            showUnhealthyWarning()
        } else {
            hideUnhealthyWarning()
        }

        // Create chart data
        createPieChart(healthyPercent.toFloat(), unhealthyPercent.toFloat())
    }

    private fun showUnhealthyWarning() {
        unhealthyWarning.visibility = View.VISIBLE
        productSearchLayout.visibility = View.VISIBLE
    }

    private fun hideUnhealthyWarning() {
        unhealthyWarning.visibility = View.GONE
        productSearchLayout.visibility = View.GONE
        alternativesRecyclerView.visibility = View.GONE
    }

    private fun setupAlternativesRecyclerView() {
        alternativesAdapter = HealthyAlternativesAdapter { alternative ->
            // Handle click on buying link
            if (alternative.buyingLink.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alternative.buyingLink))
                startActivity(intent)
            }
        }

        alternativesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ResultActivity)
            adapter = alternativesAdapter
        }
    }

    private fun createPieChart(healthyPercent: Float, unhealthyPercent: Float) {
        val entries = ArrayList<PieEntry>()

        // Add entries (only add non-zero values to avoid empty slices)
        if (healthyPercent > 0) {
            entries.add(PieEntry(healthyPercent, "Healthy"))
        }
        if (unhealthyPercent > 0) {
            entries.add(PieEntry(unhealthyPercent, "Unhealthy"))
        }

        // Handle edge case where both percentages might be 0
        if (entries.isEmpty()) {
            entries.add(PieEntry(100f, "Unknown"))
        }

        val dataSet = PieDataSet(entries, "Health Analysis").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = MPPointF(0f, 40f)
            selectionShift = 5f
        }

        // Colors based on what data we have
        val colors = ArrayList<Int>()
        for (entry in entries) {
            when (entry.label) {
                "Healthy" -> colors.add(Color.parseColor("#10B981")) // Green
                "Unhealthy" -> colors.add(Color.parseColor("#EF4444")) // Red
                else -> colors.add(Color.parseColor("#6B7280")) // Gray for unknown
            }
        }
        dataSet.colors = colors

        // Value formatting
        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter())
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
            setValueTypeface(Typeface.DEFAULT_BOLD)
        }

        pieChart.data = data

        // Update center text based on health status
        val centerText = when {
            healthyPercent >= 70 -> "Healthy\n${healthyPercent.toInt()}%"
            healthyPercent >= 50 -> "Moderate\n${healthyPercent.toInt()}%"
            else -> "Unhealthy\n${unhealthyPercent.toInt()}%"
        }
        pieChart.centerText = centerText

        // Set center text color based on health status
        val centerTextColor = when {
            healthyPercent >= 70 -> Color.parseColor("#10B981") // Green
            healthyPercent >= 50 -> Color.parseColor("#F59E0B") // Yellow
            else -> Color.parseColor("#EF4444") // Red
        }
        pieChart.setCenterTextColor(centerTextColor)

        // Refresh and animate
        pieChart.highlightValues(null)
        pieChart.invalidate()

        // Animate
        pieChart.animateY(1400, Easing.EaseInOutQuad)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        scanAgainButton.setOnClickListener {
            // Go back to scan activity
            finish()
        }

        saveResultButton.setOnClickListener {
            saveAnalysisResult()
        }

        searchButton.setOnClickListener {
            val productCategory = productSearchInput.text.toString().trim()
            if (productCategory.isNotEmpty()) {
                searchHealthyAlternatives(productCategory)
            } else {
                Toast.makeText(this, "Please enter a product category", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchHealthyAlternatives(productCategory: String) {
        loadingIndicator.visibility = View.VISIBLE
        alternativesRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val alternatives = geminiHelper.getHealthyAlternatives(productCategory)

                loadingIndicator.visibility = View.GONE

                if (alternatives.isNotEmpty()) {
                    alternativesAdapter.updateAlternatives(alternatives)
                    alternativesRecyclerView.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@ResultActivity, "No alternatives found", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this@ResultActivity, "Error fetching alternatives: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAnalysisResult() {
        // Get current data
        val healthStatus = intent.getStringExtra(EXTRA_HEALTH_STATUS) ?: "Unknown"
        val unhealthyPercent = intent.getIntExtra(EXTRA_UNHEALTHY_PERCENT, 0)
        val ingredients = intent.getStringExtra(EXTRA_INGREDIENTS) ?: ""
        val healthyPercent = 100 - unhealthyPercent

        // Save to SharedPreferences
        val sharedPref = getSharedPreferences("food_analysis_results", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("last_health_status", healthStatus)
            putInt("last_healthy_percent", healthyPercent)
            putInt("last_unhealthy_percent", unhealthyPercent)
            putString("last_ingredients", ingredients)
            putLong("last_analysis_time", System.currentTimeMillis())
            apply()
        }

        Toast.makeText(this, "Result saved successfully!", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_HEALTH_STATUS = "health_status"
        const val EXTRA_UNHEALTHY_PERCENT = "unhealthy_percent"
        const val EXTRA_INGREDIENTS = "ingredients"
        const val EXTRA_ANALYSIS_RESULT = "analysis_result"
    }
}