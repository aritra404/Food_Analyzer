package aritra.seal.food_analyzer

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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


        val previewView = findViewById<PreviewView>(R.id.previewView)
        val captureButton = findViewById<Button>(R.id.captureButton)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                101
            )
        } else {
            // Permission already granted, start the camera
            startCamera(previewView)
        }


        captureButton.setOnClickListener {
            takePhoto()
        }

        // Initialize views
        ingredientsInput = findViewById(R.id.ingredientsInput)
        analyzeButton = findViewById(R.id.analyzeButton)
        resultText = findViewById(R.id.resultText)
        scanButton = findViewById(R.id.scanButton)

        // Initialize Gemini API helper with API Key
        geminiApiHelper = GeminiApiHelper("AIzaSyBTPSWqcv1bPsghiM4gRttVL3xYhFvqB84")

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
    private var imageCapture: ImageCapture? = null

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File.createTempFile("captured", ".jpg", cacheDir)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    startCrop(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    resultText.text = "Capture failed: ${exception.message}"
                }
            }
        )
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
            val (healthStatus, unhealthyPercent) = geminiApiHelper.analyzeIngredients(ingredients)
            if (healthStatus == "Error") {
                resultText.text = "Analysis failed."
            } else {
                val message = buildString {
                    append("Status: $healthStatus\n")
                    if (unhealthyPercent != null) append("Unhealthy Percentage: $unhealthyPercent%")
                }
                resultText.text = message
                // Navigate to ResultActivity with proper data
                navigateToResultScreen(healthStatus, unhealthyPercent, ingredients)
            }
        }
    }

    private fun navigateToResultScreen(healthStatus: String, unhealthyPercent: Int?, ingredients: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_HEALTH_STATUS, healthStatus)
            putExtra(ResultActivity.EXTRA_UNHEALTHY_PERCENT, unhealthyPercent ?: 0)
            putExtra(ResultActivity.EXTRA_INGREDIENTS, ingredients)
        }
        startActivity(intent)
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
        tempFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return Uri.fromFile(tempFile)
    }
}