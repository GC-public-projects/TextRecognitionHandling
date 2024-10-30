package com.example.textrecognitionhandling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.textrecognitionhandling.ui.theme.TextRecognitionHandlingTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

class MainActivity : ComponentActivity() {
    private var inputStream: InputStream? = null
    private var bitmap by mutableStateOf<Bitmap?>(null)
    private var visionText by mutableStateOf<Text?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch image picker
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        inputStream = contentResolver.openInputStream(uri)
                        bitmap = BitmapFactory.decodeStream(inputStream)

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


// Composable to display image and highlight recognized text
@Composable
fun HighlightedImage(bitmap: Bitmap, visionText: Text) {
    val myViewModel: MyViewModel = viewModel()
    val myBlocksList by myViewModel.myBlocksList.collectAsStateWithLifecycle()
    val uiTrigger by myViewModel.uiStateTrigger.collectAsStateWithLifecycle()

    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }


    Box(modifier = Modifier.fillMaxSize()) {
        Log.i("myLog", "$uiTrigger")
        Log.i("myLog", "$myBlocksList")
        Log.i("myLog", "Box composed")
        // Display the image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                    // Get the size of the Image composable
                    viewWidth = layoutCoordinates.size.width.toFloat()
                    viewHeight = layoutCoordinates.size.height.toFloat()

                    if (viewWidth != 0f && viewHeight != 0f) {
                        if (myBlocksList.isEmpty()) {
                            Log.i("myLog", "myBlockList is empty")
                            myViewModel.fillMyBlockList(
                                visionText,
                                bitmap.width,
                                bitmap.height,
                                viewWidth,
                                viewHeight
                            )
                        }

                    }
                },
            contentScale = ContentScale.Fit
        )

        // Overlay for drawing text highlights
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset -> myViewModel.handleTap(offset) }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            myViewModel.handleDrag(change.position)
                            change.consume()
                        }
                    )
                }
        ) {
            Log.i("myLog", "$visionText")
            drawHighlights(myBlocksList)
        }
    }
}

// Function to draw the bounding boxes for text
fun DrawScope.drawHighlights(
    myBlockList: List<MyBlock>
) {
    Log.i("myLog", "drawHighlights called")

    myBlockList.forEach { myBlock ->
        myBlock.myLinesList.forEach { myLine ->
            myLine.myElementsList.forEach { myElement ->
                drawRect(
                    color = myElement.color,
                    topLeft = androidx.compose.ui.geometry.Offset(myElement.left, myElement.top),
                    size = androidx.compose.ui.geometry.Size(
                        myElement.right - myElement.left, myElement.bottom - myElement.top
                    ),
                    // style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
        }
    }
}

data class MyBlock(
    val block: Text.TextBlock,
    var color: Color,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val myLinesList: MutableList<MyLine> = mutableListOf()
}

data class MyLine(
    val line: Text.Line,
    var color: Color,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val myElementsList: MutableList<MyElement> = mutableListOf()
}

data class MyElement(
    val element: Text.Element,
    var color: Color,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
