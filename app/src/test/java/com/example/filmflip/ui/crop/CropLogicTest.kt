package com.example.filmflip.ui.crop

import org.junit.Assert.*
import org.junit.Test

class CropLogicTest {

    private val canvasW = 1000f
    private val canvasH = 1000f
    private val hitRadius = 36f

    @Test
    fun `handle positions for full crop`() {
        val positions = computeHandlePositions(0f, 0f, 1f, 1f, canvasW, canvasH)
        assertEquals(8, positions.size)
        assertEquals(androidx.compose.ui.geometry.Offset(0f, 0f), positions[0])
        assertEquals(androidx.compose.ui.geometry.Offset(500f, 0f), positions[1])
        assertEquals(androidx.compose.ui.geometry.Offset(1000f, 0f), positions[2])
        assertEquals(androidx.compose.ui.geometry.Offset(1000f, 1000f), positions[7])
    }

    @Test
    fun `handle positions for half crop`() {
        val positions = computeHandlePositions(0.25f, 0.25f, 0.75f, 0.75f, canvasW, canvasH)
        assertEquals(androidx.compose.ui.geometry.Offset(250f, 250f), positions[0])
        assertEquals(androidx.compose.ui.geometry.Offset(750f, 750f), positions[7])
    }

    @Test
    fun `hit test finds corner handle`() {
        val result = findHandleAt(0f, 0f, 1f, 1f, canvasW, canvasH, hitRadius, 10f, 10f)
        assertEquals(HANDLE_TOP_LEFT, result)
    }

    @Test
    fun `hit test finds top edge handle`() {
        val result = findHandleAt(0f, 0f, 1f, 1f, canvasW, canvasH, hitRadius, 500f, 10f)
        assertEquals(HANDLE_TOP_EDGE, result)
    }

    @Test
    fun `hit test finds bottom right handle`() {
        val result = findHandleAt(0f, 0f, 1f, 1f, canvasW, canvasH, hitRadius, 990f, 990f)
        assertEquals(HANDLE_BOTTOM_RIGHT, result)
    }

    @Test
    fun `hit test returns null when far from handles`() {
        val result = findHandleAt(0f, 0f, 1f, 1f, canvasW, canvasH, hitRadius, 500f, 500f)
        assertNull(result)
    }

    @Test
    fun `hit test returns null when outside hit radius`() {
        val result = findHandleAt(0f, 0f, 1f, 1f, canvasW, canvasH, hitRadius, 100f, 100f)
        assertNull(result)
    }

    @Test
    fun `move top left handle right and down`() {
        val result = moveCropHandle(0f, 0f, 1f, 1f, canvasW, canvasH, HANDLE_TOP_LEFT, 100f, 100f)
        assertEquals(0.1f, result.left, 0.001f)
        assertEquals(0.1f, result.top, 0.001f)
        assertEquals(1f, result.right, 0.001f)
        assertEquals(1f, result.bottom, 0.001f)
    }

    @Test
    fun `move bottom right handle left and up`() {
        val result = moveCropHandle(0f, 0f, 1f, 1f, canvasW, canvasH, HANDLE_BOTTOM_RIGHT, -100f, -100f)
        assertEquals(0f, result.left, 0.001f)
        assertEquals(0f, result.top, 0.001f)
        assertEquals(0.9f, result.right, 0.001f)
        assertEquals(0.9f, result.bottom, 0.001f)
    }

    @Test
    fun `move handle does not exceed left boundary`() {
        val result = moveCropHandle(0.1f, 0.1f, 0.5f, 0.5f, canvasW, canvasH, HANDLE_TOP_LEFT, -200f, 0f)
        assertEquals(0f, result.left, 0.001f)
    }

    @Test
    fun `move handle does not exceed right boundary`() {
        val result = moveCropHandle(0.5f, 0.5f, 0.9f, 0.9f, canvasW, canvasH, HANDLE_BOTTOM_RIGHT, 200f, 0f)
        assertEquals(1f, result.right, 0.001f)
    }

    @Test
    fun `move handle respects minimum crop size`() {
        val result = moveCropHandle(0f, 0f, 1f, 1f, canvasW, canvasH, HANDLE_TOP_LEFT, 900f, 0f)
        assertTrue(result.left <= result.right - 0.02f)
    }

    @Test
    fun `move top edge handle up`() {
        val result = moveCropHandle(0f, 0.3f, 1f, 1f, canvasW, canvasH, HANDLE_TOP_EDGE, 0f, -100f)
        assertEquals(0.2f, result.top, 0.001f)
    }

    @Test
    fun `move bottom edge handle down`() {
        val result = moveCropHandle(0f, 0f, 1f, 0.7f, canvasW, canvasH, HANDLE_BOTTOM_EDGE, 0f, 100f)
        assertEquals(0.8f, result.bottom, 0.001f)
    }

    @Test
    fun `invalid handle returns unchanged rect`() {
        val result = moveCropHandle(0.1f, 0.2f, 0.8f, 0.9f, canvasW, canvasH, 99, 50f, 50f)
        assertEquals(0.1f, result.left, 0.001f)
        assertEquals(0.2f, result.top, 0.001f)
        assertEquals(0.8f, result.right, 0.001f)
        assertEquals(0.9f, result.bottom, 0.001f)
    }

    @Test
    fun `multiple moves are composable`() {
        var crop = CropDelta(0f, 0f, 1f, 1f)

        crop = moveCropHandle(crop.left, crop.top, crop.right, crop.bottom, canvasW, canvasH, HANDLE_TOP_LEFT, 100f, 100f)
        assertEquals(0.1f, crop.left, 0.001f)
        assertEquals(0.1f, crop.top, 0.001f)

        crop = moveCropHandle(crop.left, crop.top, crop.right, crop.bottom, canvasW, canvasH, HANDLE_BOTTOM_RIGHT, -100f, -100f)
        assertEquals(0.9f, crop.right, 0.001f)
        assertEquals(0.9f, crop.bottom, 0.001f)
    }

    @Test
    fun `hit test works with small crop area`() {
        val positions = computeHandlePositions(0.4f, 0.4f, 0.6f, 0.6f, canvasW, canvasH)
        val centerTop = positions[1]
        val result = findHandleAt(0.4f, 0.4f, 0.6f, 0.6f, canvasW, canvasH, hitRadius, centerTop.x + 5f, centerTop.y + 5f)
        assertEquals(HANDLE_TOP_EDGE, result)
    }
}