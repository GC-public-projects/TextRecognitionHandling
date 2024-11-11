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
I encountered some issues with the different gestures linked to one elemnent. Indeed, in order to handle `detectDragGestures`, `detectTapGestures` and `detectTransformGestures` in the same time i cannot do that in one `pointerInput` modifier function but i also cannot do 3 different pointerInputs because some gesture are not correcly responsive. So i customized the gesture mechanism in order to reproduce the gesture behaviors in 1 `pointerInput`.

# Init

# Code
