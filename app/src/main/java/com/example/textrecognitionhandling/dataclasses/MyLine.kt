package com.example.textrecognitionhandling.dataclasses

import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.text.Text

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