package com.example.textrecognitionhandling

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MyViewModel : ViewModel() {
    private val _myBlocksList = MutableStateFlow<List<MyBlock>>(emptyList())
    val myBlocksList: StateFlow<List<MyBlock>> get() = _myBlocksList

    private val _uiStateTrigger = MutableStateFlow(0)
    val uiStateTrigger: StateFlow<Int> get() = _uiStateTrigger

    fun fillMyBlockList(
        visionText: Text,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val highlightColor = Color(1f, 0.5f, 0f, 0.5f) // Orange with 50% transparency
            val scaleX = viewWidth / imageWidth
            val scaleY = viewHeight / imageHeight

            // Choose the smaller scaling factor to maintain aspect ratio
            val scale = minOf(scaleX, scaleY)

            // Calculate offsets to center the image
            val offsetX = (viewWidth - imageWidth * scale) / 2
            val offsetY = (viewHeight - imageHeight * scale) / 2

            var left = 0f
            var top = 0f
            var right = 0f
            var bottom = 0f

            val tempList : MutableList<MyBlock> = mutableListOf()

            // Draw bounding boxes with calculated offsets and universal scaling
            for (block in visionText.textBlocks) {
                block.boundingBox?.let { rect ->
                    left = rect.left * scale + offsetX
                    top = rect.top * scale + offsetY
                    right = rect.right * scale + offsetX
                    bottom = rect.bottom * scale + offsetY
                }
                val myBlock = MyBlock(block, highlightColor, left, top, right, bottom)
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


