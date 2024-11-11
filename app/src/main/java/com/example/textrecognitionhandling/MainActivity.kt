package com.example.textrecognitionhandling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.textrecognitionhandling.dataclasses.MyBlock
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


// Composable to display image and highlight recognized text
@Composable
fun HighlightedImage(bitmap: Bitmap, visionText: Text) {
    val myViewModel: MyViewModel = viewModel()
    val myBlocksList by myViewModel.myBlocksList.collectAsStateWithLifecycle()
    val uiTrigger by myViewModel.uiStateTrigger.collectAsStateWithLifecycle()

    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    var scale by remember { mutableFloatStateOf(1f) } // State to manage zoom level
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var previousScale = scale
                var dragStartPosition: Offset? = null
                val dragThreshold = 10f // Minimum movement threshold for drag
                val tapTimeThreshold = 200L // Maximum duration (ms) to consider as a tap
                val dragHoldDelay = 150L // Delay to distinguish between tap and drag
                var initialTouchTime: Long? = null // Track the initial touch time
                var isDrag = false // Track if a drag gesture is detected

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        // Check if there is only one pointer on the screen
                        if (event.changes.size == 1) {
                            val change = event.changes.first()

                            if (change.pressed) {
                                // Track the initial touch time when the finger is pressed down
                                if (initialTouchTime == null) {
                                    initialTouchTime = System.currentTimeMillis()
                                    dragStartPosition = change.position
                                    isDrag = false // Reset drag detection
                                }

                                // Check if the delay has passed, indicating intent to drag
                                val currentTime = System.currentTimeMillis()
                                val elapsedTime = currentTime - initialTouchTime!!

                                if (elapsedTime >= dragHoldDelay && dragStartPosition != null) {
                                    // Calculate the distance moved since the start
                                    val distance = (change.position - dragStartPosition!!).getDistance()

                                    if (distance > dragThreshold) {
                                        isDrag = true
                                        myViewModel.handleDrag(change.position)
                                        change.consume() // Prevent affecting other gestures
                                    }
                                }
                            }

                            // Detect tap gesture only if it's a quick tap (within time threshold)
                            if (!change.pressed && change.changedToUp()) {
                                if (!isDrag) { // Only handle as tap if no drag was detected
                                    initialTouchTime?.let { downTime ->
                                        val tapDuration = System.currentTimeMillis() - downTime
                                        if (tapDuration <= tapTimeThreshold) {
                                            myViewModel.handleTap(change.position)
                                        }
                                    }
                                }
                                // Reset variables after tap or drag ends
                                initialTouchTime = null
                                dragStartPosition = null
                                isDrag = false
                            }
                        } else {
                            // Reset drag and tap detection when multiple fingers are detected
                            dragStartPosition = null
                            initialTouchTime = null
                            isDrag = false
                        }

                        // Detect zoom and pan gestures
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        // Update scale and pan only if zooming has occurred
                        val newScale = (scale * zoomChange).coerceIn(0.3f, 5f)
                        if (newScale != previousScale) {
                            scale = newScale
                            offsetX += panChange.x
                            offsetY += panChange.y

                            // Update block positions based on the new scale and offset
                            myViewModel.updateMyBlocksList(
                                bitmap.width,
                                bitmap.height,
                                viewWidth,
                                viewHeight,
                                scale,
                                offsetX,
                                offsetY
                            )
                        }

                        previousScale = newScale
                    }
                }
            }
    )


    {
        Log.i("myLog", "$uiTrigger")
        Log.i("myLog", "$myBlocksList")
        Log.i("myLog", "Box composed")
        // Display the image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .onGloballyPositioned { layoutCoordinates ->
                    // Get the size of the Image composable
                    viewWidth = layoutCoordinates.size.width.toFloat()
                    viewHeight = layoutCoordinates.size.height.toFloat()

                    if (viewWidth != 0f && viewHeight != 0f) {
                        if (myBlocksList.isEmpty()) {
                            Log.i("myLog", "myBlockList is empty")
                            myViewModel.fillMyBlocksList(
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
                    topLeft = Offset(myElement.left, myElement.top),
                    size = androidx.compose.ui.geometry.Size(
                        myElement.right - myElement.left, myElement.bottom - myElement.top
                    ),
                    // style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
        }
    }
}
