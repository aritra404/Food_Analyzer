package aritra.seal.food_analyzer

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

fun extractTextFromImage(image: InputImage, onTextExtracted: (String) -> Unit) {
    val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(image)
        .addOnSuccessListener { result ->
            val extractedText = result.text
            onTextExtracted(extractedText)
        }
        .addOnFailureListener { e ->
            onTextExtracted("Error: ${e.message}")
        }
}
