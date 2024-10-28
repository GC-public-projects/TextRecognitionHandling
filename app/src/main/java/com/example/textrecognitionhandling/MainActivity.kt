package com.example.textrecognitionhandling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.textrecognitionhandling.ui.theme.TextRecognitionHandlingTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream

class MainActivity : ComponentActivity() {
    var inputStream: InputStream? = null
    var bitmap: Bitmap? = null
    var visionText: Text? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch image picker
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)

                bitmap?.let { bmp ->
                    runTextRecognition(bmp) // Process text recognition
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
                        .padding(16.dp)
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

    // Run text recognition on the given bitmap
    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                visionText = result // Store recognized text
                setContent { // Refresh UI
                    TextRecognitionHandlingTheme {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Re-display the image with highlights
                            bitmap?.let { bmp ->
                                visionText?.let { text ->
                                    HighlightedImage(bmp, text)
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}

// Composable to display image and highlight recognized text
@Composable
fun HighlightedImage(bitmap: Bitmap, visionText: Text) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Display the image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Overlay for drawing text highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawHighlights(visionText, bitmap.width, bitmap.height)
        }
    }
}

// Function to draw the bounding boxes for text
fun DrawScope.drawHighlights(visionText: Text, imageWidth: Int, imageHeight: Int) {
    val highlightColor = Color(1f, 0.5f, 0f, 0.5f) // Orange with 50% transparency

    // Calculate independent scaling factors
    val scaleX = size.width / imageWidth
    val scaleY = size.height / imageHeight

    // Choose the smaller scaling factor to maintain aspect ratio
    val scale = minOf(scaleX, scaleY)

    // Calculate offsets to center the image
    val offsetX = (size.width - imageWidth * scale) / 2
    val offsetY = (size.height - imageHeight * scale) / 2

    // Draw bounding boxes with calculated offsets and universal scaling
    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            for (element in line.elements) {
                element.boundingBox?.let { rect ->
                    val left = rect.left * scale + offsetX
                    val top = rect.top * scale + offsetY
                    val right = rect.right * scale + offsetX
                    val bottom = rect.bottom * scale + offsetY

                    // Draw rectangle around the text
                    drawRect(
                        color = highlightColor,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                        // style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }
            }
        }
    }
}
