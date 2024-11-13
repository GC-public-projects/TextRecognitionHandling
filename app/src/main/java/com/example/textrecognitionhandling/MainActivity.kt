package com.example.textrecognitionhandling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.example.textrecognitionhandling.ui.theme.TextRecognitionHandlingTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var bitmap by mutableStateOf<Bitmap?>(null)
    private var visionText by mutableStateOf<Text?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch image picker
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        bitmap = adjustBitmapOrientation(uri)

                        bitmap?.let { bmp ->
                            runTextRecognition(bmp) // Process text recognition
                        }
                    }
                }
            }
        getContent.launch("image/*")

        // Set up Jetpack Compose
        setContent {
            TextRecognitionHandlingTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    // Display Image with Highlights
                    bitmap?.let { bmp ->
                        visionText?.let { text ->
                            HighlightedImage(bmp, text)
                        }
                    }
                }
            }
        }
    }

    private fun adjustBitmapOrientation(uri: Uri): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Reset the input stream to read EXIF data
        val exifInputStream = contentResolver.openInputStream(uri)
        val exif = exifInputStream?.let { ExifInterface(it) }
        val orientation =
            exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> bitmap?.rotateBitmap(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> bitmap?.rotateBitmap(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> bitmap?.rotateBitmap(270f)
            else -> bitmap
        }
    }

    private fun Bitmap.rotateBitmap(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                CoroutineScope(Dispatchers.Main).launch {
                    visionText = result // Store recognized text
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}



