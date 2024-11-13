# TextRecognitionHandling
Use of Google ML kit Text recognition V2 to select words of images


### Project status : Workable, documentation in progress...


## target audience
This project is for Kotlin Jetpack Compose initiated user.

## Presentation
Implementation of Google ML kit in order to highlight the words of a text in an image. The words dragged or tapped will be higlighted in a different color. The image can be zoomed or reduced too.
Thanks to this demo it will be easy by making some changes in the code to collect the tapped or dragged words and do what we want with them.


## Overview

- 1 : Selection of the image
- 2 : all words highlighted with default color
- 3 : words higlighted after tapped or dragged them

<img src="/screenshots/1.png" alt="Selection of the image" height="500">&emsp;
<img src="/screenshots/2.png" alt="all words highlighted with default color" height="500">&emsp;
<img src="/screenshots/3.png" alt="words higlighted after tapped or dragged them" height="500">&emsp;

## Warning 

- the accuracy of the library is not the best one. So depending the image quality and the spaces betwwen the words and the punctiation the words are more or less recognized with punctuation on them or not.

- I encountered some issues with the different gestures linked to one elemnent. Indeed, in order to handle `detectDragGestures`, `detectTapGestures` and `detectTransformGestures` in the same time i cannot do that in one `pointerInput` modifier function but i also cannot do 3 different pointerInputs because some gesture are not correcly responsive. So i customized the gesture mechanism in order to reproduce the gesture behaviors in 1 `pointerInput`.

# Init

## Dependencies
In build.gradle.kts (app) add the following ones : 
``` kotlin
dependencies {
	...

	implementation ("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    ...
}

```

# Code

## MyViewModel (class)

### Purpose 
In order to higlight some text elements (the words), we need to display the image, and on the top on it, displaying a drawscope that contents all position of the words in rectangle shapes.
All the positions of the rectangles will be stored in dataclasses. The viewModel holds the machanisms to calculate these positions in function of the image and the view measurements and positions. 

As we also want to zoom out, zoom in the image and select some words by tapping or dragging them the viewModel holds the fuctions to recalculate the rectangles positions and modify their colors in function of the getures applied.


### Content
In main package create kotlin class named `MyViewModel`
``` kotlin
class MyViewModel : ViewModel() {
    private val _myBlocksList = MutableStateFlow<List<MyBlock>>(emptyList())
    val myBlocksList: StateFlow<List<MyBlock>> get() = _myBlocksList

    private val _uiStateTrigger = MutableStateFlow(0)
    val uiStateTrigger: StateFlow<Int> get() = _uiStateTrigger

        fun fillMyBlocksList(
        visionText: Text,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val highlightColor = Color(1f, 0.5f, 0f, 0.5f) // Orange with 50% transparency

            val scale: Float = calculateScale(
                imageWidth,
                imageHeight,
                viewWidth,
                viewHeight
            )
            val offsets: Pair<Float, Float> = calculateOffsets(
                scale,
                imageWidth,
                imageHeight,
                viewWidth,
                viewHeight,
            )
            val offsetX = offsets.first
            val offsetY = offsets.second

            var left = 0f
            var top = 0f
            var right = 0f
            var bottom = 0f

            val tempList: MutableList<MyBlock> = mutableListOf()

            // Draw bounding boxes with calculated offsets and universal scaling
            for (block in visionText.textBlocks) {
                block.boundingBox?.let { rect ->
                    left = rect.left * scale + offsetX
                    top = rect.top * scale + offsetY
                    right = rect.right * scale + offsetX
                    bottom = rect.bottom * scale + offsetY
                }
                val myBlock = com.example.textrecognitionhandling.dataclasses.MyBlock(
                    block,
                    highlightColor,
                    left,
                    top,
                    right,
                    bottom
                )
                tempList.add(myBlock)

                for (line in block.lines) {
                    line.boundingBox?.let { rect ->
                        left = rect.left * scale + offsetX
                        top = rect.top * scale + offsetY
                        right = rect.right * scale + offsetX
                        bottom = rect.bottom * scale + offsetY
                    }
                    val myLine = MyLine(line, highlightColor, left, top, right, bottom)
                    myBlock.myLinesList.add(myLine)

                    for (element in line.elements) {
                        element.boundingBox?.let { rect ->
                            left = rect.left * scale + offsetX
                            top = rect.top * scale + offsetY
                            right = rect.right * scale + offsetX
                            bottom = rect.bottom * scale + offsetY
                        }
                        val myElement = MyElement(element, highlightColor, left, top, right, bottom)
                        myLine.myElementsList.add(myElement)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                _myBlocksList.value = tempList
            }
        }


    }

    fun updateMyBlocksList(
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        zoom: Float = 1f,
        moveX: Float = 0f,
        moveY: Float = 0f
    ) {
        val scale: Float = calculateScale(
            imageWidth,
            imageHeight,
            viewWidth,
            viewHeight,
            zoom
        )
        val offsets: Pair<Float, Float> = calculateOffsets(
            scale,
            imageWidth,
            imageHeight,
            viewWidth,
            viewHeight,
            moveX,
            moveY
        )
        val offsetX = offsets.first
        val offsetY = offsets.second

        _myBlocksList.value.forEach { myBlock ->
            myBlock.block.boundingBox?.let { rect ->
                myBlock.left = rect.left * scale + offsetX
                myBlock.top = rect.top * scale + offsetY
                myBlock.right = rect.right * scale + offsetX
                myBlock.bottom = rect.bottom * scale + offsetY

                myBlock.myLinesList.forEach { myLine ->
                    myLine.line.boundingBox?.let { rect ->
                        myLine.left = rect.left * scale + offsetX
                        myLine.top = rect.top * scale + offsetY
                        myLine.right = rect.right * scale + offsetX
                        myLine.bottom = rect.bottom * scale + offsetY

                        myLine.myElementsList.forEach { myElement ->
                            myElement.element.boundingBox?.let { rect ->
                                myElement.left = rect.left * scale + offsetX
                                myElement.top = rect.top * scale + offsetY
                                myElement.right = rect.right * scale + offsetX
                                myElement.bottom = rect.bottom * scale + offsetY
                            }
                        }
                    }
                }
            }
        }
    }

    private fun calculateScale(
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        zoom: Float = 1f
    ): Float {
        val scaleX = viewWidth / imageWidth * zoom
        val scaleY = viewHeight / imageHeight * zoom
        return minOf(scaleX, scaleY)
    }

    private fun calculateOffsets(
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        moveX: Float = 0f,
        moveY: Float = 0f
    ): Pair<Float, Float> {
        val offsetX = if((viewWidth - imageWidth * scale) == 0f ) moveX else (viewWidth - imageWidth * scale) / 2 + moveX
        val offsetY = if((viewHeight - imageHeight * scale) == 0f) moveY else (viewHeight - imageHeight * scale) / 2 + moveY
        return Pair(offsetX, offsetY)
    }

    // Function to handle tap gesture
    fun handleTap(offset: Offset) {
        // Check which element was tapped
        Log.i("myGesture", "gesture tap")
        targetElementAndChangeColor(
            offset,
            Color(0f, 0f, 1f, 0.5f)
        ) // Change to blue with 50% transparency
    }

    // Function to handle drag gesture
    fun handleDrag(offset: Offset) {
        // Check which element is dragged over
        Log.i("myGesture", "gesture drag")
        targetElementAndChangeColor(
            offset,
            Color(1f, 0f, 0f, 0.5f)
        ) // Change to red with 50% transparency
    }

    private fun targetElementAndChangeColor(offset: Offset, color: Color) {
        _myBlocksList.value.forEach { myBlock ->
            myBlock.myLinesList.forEach { myLine ->
                myLine.myElementsList.forEach { myElement ->
                    if (offset.x in myElement.left..myElement.right && offset.y in myElement.top..myElement.bottom) {
                        Log.i("myLog", "element found : $myElement")
                        myElement.color = color
                    }
                }
            }
        }
        _uiStateTrigger.value += 1
    }
}
```

### Components explanations

`\_myBlocksList` & `myBlocksList` : list with its getter that contains all the blocks generated by GoogleMlkit Text recognition V2. Each block, line and element are stored in a dataclass with the rectangle postions relative to our view.

`\_uiStateTrigger ` & `uiStateTrigger` : stateFlow variable with its getter useful the trigger the recompositions of the UI when the elements of the drawscope (dataclasses stored in \_myBlocksList) are modified.
In an ideal world the modification of `\_myBlocksList` should be enough to trigger the ui recompositions as it is of type `MutableStateFlow` but in fact that is not that simple. Updating elements in the list is not recognized as a change as the memory adress of the list inside the stateFlow is still the same. theoretically it is possible to recreate for each UI change a new list that will be affected to the flow but is is heavy to implement and it is just more simple to use `_uiStateTrigger` as just an increment of its value is enough to recompose the UI. 


`fillMyBlocksList` : Public function is called in the `.onGloballyPositioned` Modifier method of the Image composable in order to have the dimensions of the view (Image modified by Contescale.fit) when it is created. This function create the datafiles in different sorted lists that respect the structure of `Ml kit text recognitions TextBlocks` and add them to `\_myBlocksList`.

`updateMyBlocksList` : public function that updates the positions of the left, right, bottom, top rectangles of the datafiles when the image is moved or zoomed.

`calculateScale` : private function that calculates how much the rectangles provided by Textrecognition V2 need to be reduced or expanded in function of the measurments of the image modified.

`calculateOffsets` : private function that calculates how much the rectangles provided by Textrecognition V2 need to be moved in function of the position and the smeasurments of the image modified. the function return a pair of offsets to move the rectangle vertically and horizontally.

`handleTap` : public function called when a tap gesture is recognized. it provides the offset of the tap and teh color to apply to the rectangle that surround the point.


`handleDrag` : public function called when a drag gesture is recognized. it provides the offset of the dragged element and the color to apply to the rectangle that surround the point.

`targetElementAndChangeColor` : private function that changes the color of a rectangle thanks to an offset of the view (image modified).

## Package dataclasses
Package created in the Main package with inside 3 different dataclasses.

The dataClasses content for each kind of element of `visionText.textBlocks` the colors and the postions of the rectangles relative to the modified Image. Each data class also contents its respective element it is attached in order to calculate the positions of the new rectangles after Ui modifiaction. this element can also be used of course to capture the text inside and reused it after.  Each `Mylock` contents a list of `MyLine` that contents a list of `MyElement`, exactly like the TextBlocks from visionText.

The color, left, top, bottom, right values from MyBlock and MyLine dataclasses are not used as we just surround the `Text.Element` (words) but they are implemented in case in the future we want to use them.


### MyBlock (data class)

#### Content
``` kotlin
data class MyBlock(
    val block: Text.TextBlock,
    var color: Color,
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
) {
    val myLinesList: MutableList<MyLine> = mutableListOf()
}
```

### MyLine (data class)

#### Content
``` kotlin
data class MyLine(
    val line: Text.Line,
    var color: Color,
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
) {
    val myElementsList: MutableList<MyElement> = mutableListOf()
}
```

### MyElement (data class)

#### Content
``` kotlin
data class MyElement(
    val element: `Text.Element`,
    var color: Color,
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
)
```

## HighlightedImage
Composable to displays an image and highlights recognized text

### Content 
In Main package create Kotlin file named `HighlightedImage`


``` kotlin
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
```
- `myViewModel` : instance of the viewModel
- `myBlocksList` : the list that contents the positions, colors and text of our rectangles
- `uiTrigger` : The key element to recompose the view when changes are done in `myBlocksList` 
- `viewWidth` :
- `viewHeight` :
- `scale` :
- `offsetX` :
- `offsetY` :



### Components explanations

## DrawScope.drawHighlights 
Function to draw the bounding boxes for text

### Content
on the same file of the composable just behind create a custom function for `Drawscope` named `DrawScope.drawHighlights`

``` kotlin
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
```

## MainActivity

### Content
``` kotlin
```