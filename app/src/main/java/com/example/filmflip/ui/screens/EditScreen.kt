package com.example.filmflip.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.filmflip.R
import com.example.filmflip.processor.CropRect
import com.example.filmflip.ui.crop.*
import com.example.filmflip.viewmodel.FilmFlipViewModel

enum class EditMode { Adjust, Crop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(viewModel: FilmFlipViewModel) {
    val context = LocalContext.current

    var showSavedDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf<EditMode>(EditMode.Adjust) }

    LaunchedEffect(viewModel.saveResult) {
        if (viewModel.saveResult == FilmFlipViewModel.SaveResult.Success) {
            showSavedDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FilmFlip") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goToHome() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Go home"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetParameters() },
                        enabled = !viewModel.isProcessing
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_reset),
                            contentDescription = "Reset settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (!viewModel.isProcessing && viewModel.processedBitmap != null) {
                FloatingActionButton(
                    onClick = { viewModel.saveToGallery(context) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_save),
                        contentDescription = "Save"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.isProcessing) {
                    CircularProgressIndicator()
                } else if (viewModel.processedBitmap != null) {
                    if (editMode == EditMode.Crop) {
                        CropOverlay(
                            bitmap = viewModel.processedBitmap!!,
                            cropRect = viewModel.cropRect,
                            onCropChange = { viewModel.updateCropRect(it) }
                        )
                    } else {
                        Image(
                            bitmap = viewModel.processedBitmap!!.asImageBitmap(),
                            contentDescription = "Processed image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No image",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAdjust = editMode == EditMode.Adjust
                val isCrop = editMode == EditMode.Crop

                Button(
                    onClick = {
                        editMode = EditMode.Adjust
                        viewModel.applyCrop()
                    },
                    enabled = !viewModel.isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdjust) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Adjust")
                }

                Button(
                    onClick = {
                        editMode = EditMode.Crop
                        viewModel.startCropping()
                    },
                    enabled = !viewModel.isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCrop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Crop")
                }
            }

            if (editMode == EditMode.Crop) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.applyCrop() },
                        modifier = Modifier.weight(1f),
                        enabled = !viewModel.isProcessing
                    ) {
                        Text("Apply")
                    }
                    Button(
                        onClick = { viewModel.resetCrop() },
                        modifier = Modifier.weight(1f),
                        enabled = !viewModel.isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Reset")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { viewModel.rotateLeft() },
                        enabled = !viewModel.isProcessing
                    ) {
                        Text("↺", style = MaterialTheme.typography.headlineMedium)
                    }
                    Text(
                        text = if (viewModel.rotation == 0) "0°" else "${viewModel.rotation}°",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = { viewModel.rotateRight() },
                        enabled = !viewModel.isProcessing
                    ) {
                        Text("↻", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                if (editMode == EditMode.Adjust) {
                    SliderRow(
                        label = "Gamma",
                        value = viewModel.gamma,
                        onValueChange = { viewModel.setGamma(it) },
                        valueRange = 0.5f..3f,
                        steps = 50,
                        isEnabled = !viewModel.isProcessing
                    )

                    SliderRow(
                        label = "Contrast",
                        value = viewModel.contrast,
                        onValueChange = { viewModel.setContrast(it) },
                        valueRange = -0.5f..0.5f,
                        steps = 50,
                        isEnabled = !viewModel.isProcessing
                    )

                    SliderRow(
                        label = "Brightness",
                        value = viewModel.brightness,
                        onValueChange = { viewModel.setBrightness(it) },
                        valueRange = -100f..100f,
                        steps = 50,
                        isEnabled = !viewModel.isProcessing
                    )

                    SliderRow(
                        label = "Warmth",
                        value = viewModel.warmth,
                        onValueChange = { viewModel.setWarmth(it) },
                        valueRange = -0.5f..0.5f,
                        steps = 50,
                        isEnabled = !viewModel.isProcessing
                    )
                }
            }
        }
    }

    if (showSavedDialog) {
        Dialog(onDismissRequest = { showSavedDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Photo saved to gallery",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
internal fun CropOverlay(
    bitmap: Bitmap,
    cropRect: CropRect,
    onCropChange: (CropRect) -> Unit
) {
    val density = LocalDensity.current
    val handleRadius = with(density) { 14.dp.toPx() }
    val hitRadius = with(density) { 40.dp.toPx() }

    var draggingHandle by remember { mutableStateOf<Int?>(null) }
    var currentCrop by remember { mutableStateOf(cropRect) }

    LaunchedEffect(cropRect) {
        currentCrop = cropRect
    }


    val imageBitmap = bitmap.asImageBitmap()
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 8.dp)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        var imgW = maxWidth
        var imgH = maxWidth / aspectRatio
        if (imgH > maxHeight) {
            imgH = maxHeight
            imgW = maxHeight * aspectRatio
        }
        val imgWidthDp = with(density) { imgW.toDp() }
        val imgHeightDp = with(density) { imgH.toDp() }

        fun hitTest(pos: Offset): Int? {
            val positions = computeHandlePositions(
                currentCrop.left, currentCrop.top, currentCrop.right, currentCrop.bottom,
                imgW, imgH
            )
            val r2 = hitRadius * hitRadius
            for (i in positions.indices) {
                val dx = pos.x - positions[i].x
                val dy = pos.y - positions[i].y
                if (dx * dx + dy * dy <= r2) return i
            }
            return null
        }

        fun moveHandle(handle: Int, dragAmount: Offset) {
            val ndx = dragAmount.x / imgW
            val ndy = dragAmount.y / imgH
            val min = 0.02f
            val left = currentCrop.left
            val top = currentCrop.top
            val right = currentCrop.right
            val bottom = currentCrop.bottom
            val newRect = when (handle) {
                0 -> CropDelta(
                    (left + ndx).coerceIn(0f, right - min),
                    (top + ndy).coerceIn(0f, bottom - min),
                    right, bottom
                )
                1 -> CropDelta(
                    left,
                    (top + ndy).coerceIn(0f, bottom - min),
                    right, bottom
                )
                2 -> CropDelta(
                    left,
                    (top + ndy).coerceIn(0f, bottom - min),
                    (right + ndx).coerceIn(left + min, 1f),
                    bottom
                )
                3 -> CropDelta(
                    left, top,
                    (right + ndx).coerceIn(left + min, 1f),
                    (bottom + ndy).coerceIn(top + min, 1f)
                )
                4 -> CropDelta(
                    (left + ndx).coerceIn(0f, right - min),
                    (top + ndy).coerceIn(0f, bottom - min),
                    right, bottom
                )
                5 -> CropDelta(
                    left,
                    (top + ndy).coerceIn(0f, bottom - min),
                    (right + ndx).coerceIn(left + min, 1f),
                    bottom
                )
                6 -> CropDelta(
                    (left + ndx).coerceIn(0f, right - min),
                    top,
                    right,
                    (bottom + ndy).coerceIn(top + min, 1f)
                )
                7 -> CropDelta(
                    left, top,
                    (right + ndx).coerceIn(left + min, 1f),
                    (bottom + ndy).coerceIn(top + min, 1f)
                )
                else -> CropDelta(left, top, right, bottom)
            }
            currentCrop = CropRect(newRect.left, newRect.top, newRect.right, newRect.bottom)
            onCropChange(currentCrop)
        }

        Box(
            modifier = Modifier
                .width(imgWidthDp)
                .height(imgHeightDp)
                .pointerInput(hitRadius) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down = event.changes.firstOrNull { it.pressed && !it.isConsumed } ?: continue
                            down.consume()

                            val handle = hitTest(down.position)
                            if (handle != null) {
                                draggingHandle = handle
                                var prevPos = down.position

                                do {
                                    val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = dragEvent.changes.firstOrNull() ?: break
                                    if (change.positionChanged()) {
                                        change.consume()
                                        val dx = change.position.x - prevPos.x
                                        val dy = change.position.y - prevPos.y
                                        moveHandle(handle, Offset(dx, dy))
                                        prevPos = change.position
                                    }
                                } while (dragEvent.changes.any { it.pressed })

                                draggingHandle = null
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Cropping",
                modifier = Modifier.matchParentSize()
            )

            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val cx = currentCrop.left * imgW
                val cy = currentCrop.top * imgH
                val cw = (currentCrop.right - currentCrop.left) * imgW
                val ch = (currentCrop.bottom - currentCrop.top) * imgH

                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset.Zero,
                    size = Size(imgW, cy)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(0f, cy + ch),
                    size = Size(imgW, imgH - cy - ch)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(0f, cy),
                    size = Size(cx, ch)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(cx + cw, cy),
                    size = Size(imgW - cx - cw, ch)
                )

                drawRect(
                    color = Color(0xFFFF5722),
                    topLeft = Offset(cx, cy),
                    size = Size(cw, ch),
                    style = Stroke(width = 2f)
                )

                val positions = computeHandlePositions(
                    currentCrop.left, currentCrop.top, currentCrop.right, currentCrop.bottom,
                    imgW, imgH
                )

                positions.forEach { pos ->
                    drawCircle(
                        color = Color.White,
                        radius = handleRadius,
                        center = pos
                    )
                    drawCircle(
                        color = Color(0xFFFF5722),
                        radius = handleRadius * 0.6f,
                        center = pos
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isEnabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = isEnabled
        )
    }
}