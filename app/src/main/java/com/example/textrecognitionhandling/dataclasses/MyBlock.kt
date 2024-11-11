package com.example.textrecognitionhandling.dataclasses

import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.text.Text

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