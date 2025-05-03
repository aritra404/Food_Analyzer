package aritra.seal.food_analyzer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ScanActivity : AppCompatActivity() {

    private lateinit var ingredientsInput: EditText
    private lateinit var analyzeButton: Button
    private lateinit var scanButton: Button
    private lateinit var resultText: TextView
    private lateinit var geminiApiHelper: GeminiApiHelper

    val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Activity Result Launcher for Camera Intent
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    startCrop(imageUri)
                } else {
                    val imageBitmap = result.data?.extras?.get("data") as Bitmap
                    val tempUri = saveBitmapToFile(imageBitmap)
                    startCrop(tempUri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Initialize views
        ingredientsInput = findViewById(R.id.ingredientsInput)
        analyzeButton = findViewById(R.id.analyzeButton)
        resultText = findViewById(R.id.resultText)
        scanButton = findViewById(R.id.scanButton)

        // Initialize Gemini API helper with API Key
        geminiApiHelper = GeminiApiHelper("YOUR_GEMINI_API_KEY")

        // Button to analyze manually entered ingredients
        analyzeButton.setOnClickListener {
            val ingredients = ingredientsInput.text.toString()
            if (ingredients.isNotEmpty()) {
                analyzeIngredients(ingredients)
            } else {
                resultText.text = "Please enter the ingredient list."
            }
        }

        // Button to open camera and scan ingredients
        scanButton.setOnClickListener {
            openCamera()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))

        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)  // Allow free cropping without aspect ratio constraints
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(100)
        }

        UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .start(this)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                processImageFromUri(resultUri)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            resultText.text = "Error cropping image: ${cropError?.message}"
        }
    }

    private fun processImageFromUri(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        processImageForText(bitmap)
    }

    private fun processImageForText(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        resultText.text = "Scanning..."

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                ingredientsInput.setText(extractedText)
                resultText.text = "Text Scanned Successfully! Click Analyze."
            }
            .addOnFailureListener { e ->
                resultText.text = "Error scanning text: ${e.message}"
            }
    }

    private fun analyzeIngredients(ingredients: String) {
        resultText.text = "Analyzing..."
        CoroutineScope(Dispatchers.Main).launch {
            val result = geminiApiHelper.analyzeIngredients(ingredients)
            resultText.text = result
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
        tempFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return Uri.fromFile(tempFile)
    }
}

