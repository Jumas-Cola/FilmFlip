package org.jumascola.filmflip.viewmodel

import com.example.filmflip.processor.CropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class FilmFlipViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FilmFlipViewModel {
        return FilmFlipViewModel()
    }

    @Test
    fun `initial screen is Home`() {
        val vm = createViewModel()
        assertTrue(vm.currentScreen is AppScreen.Home)
    }

    @Test
    fun `goToCamera sets Camera screen`() {
        val vm = createViewModel()
        vm.goToCamera()
        assertTrue(vm.currentScreen is AppScreen.Camera)
    }

    @Test
    fun `goToBacklight sets Backlight screen`() {
        val vm = createViewModel()
        vm.goToBacklight()
        assertTrue(vm.currentScreen is AppScreen.Backlight)
    }

    @Test
    fun `gamma defaults to expected value`() {
        val vm = createViewModel()
        assertEquals(1.4f, vm.gamma, 0.01f)
    }

    @Test
    fun `setGamma updates value`() {
        val vm = createViewModel()
        vm.setGamma(2.0f)
        assertEquals(2.0f, vm.gamma, 0.01f)
    }

    @Test
    fun `setContrast updates value`() {
        val vm = createViewModel()
        vm.setContrast(0.3f)
        assertEquals(0.3f, vm.contrast, 0.01f)
    }

    @Test
    fun `setBrightness updates value`() {
        val vm = createViewModel()
        vm.setBrightness(50f)
        assertEquals(50f, vm.brightness, 0.01f)
    }

    @Test
    fun `setWarmth updates value`() {
        val vm = createViewModel()
        vm.setWarmth(-0.2f)
        assertEquals(-0.2f, vm.warmth, 0.01f)
    }

    @Test
    fun `rotateRight increments by 90`() {
        val vm = createViewModel()
        assertEquals(0, vm.rotation)
        vm.rotateRight()
        assertEquals(90, vm.rotation)
        vm.rotateRight()
        assertEquals(180, vm.rotation)
        vm.rotateRight()
        assertEquals(270, vm.rotation)
        vm.rotateRight()
        assertEquals(0, vm.rotation)
    }

    @Test
    fun `rotateLeft decrements by 90`() {
        val vm = createViewModel()
        assertEquals(0, vm.rotation)
        vm.rotateLeft()
        assertEquals(270, vm.rotation)
        vm.rotateLeft()
        assertEquals(180, vm.rotation)
    }

    @Test
    fun `resetParameters resets all values`() {
        val vm = createViewModel()
        vm.setGamma(2.0f)
        vm.setContrast(0.5f)
        vm.setBrightness(50f)
        vm.setWarmth(-0.3f)
        vm.rotateRight()
        vm.updateCropRect(CropRect(left = 0.1f))

        vm.resetParameters()

        assertEquals(1.4f, vm.gamma, 0.01f)
        assertEquals(0.15f, vm.contrast, 0.01f)
        assertEquals(0f, vm.brightness, 0.01f)
        assertEquals(0.1f, vm.warmth, 0.01f)
        assertEquals(0, vm.rotation)
        assertEquals(CropRect(), vm.cropRect)
    }

    @Test
    fun `resetRotation sets to zero`() {
        val vm = createViewModel()
        vm.rotateRight()
        vm.rotateRight()
        vm.resetRotation()
        assertEquals(0, vm.rotation)
    }

    @Test
    fun `updateCropRect updates crop`() {
        val vm = createViewModel()
        val rect = CropRect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        vm.updateCropRect(rect)
        assertEquals(rect, vm.cropRect)
    }

    @Test
    fun `resetCrop resets crop to default`() {
        val vm = createViewModel()
        vm.updateCropRect(CropRect(left = 0.1f))
        vm.resetCrop()
        assertEquals(CropRect(), vm.cropRect)
        assertFalse(vm.isCropping)
    }

    @Test
    fun `startCropping sets flag`() {
        val vm = createViewModel()
        assertFalse(vm.isCropping)
        vm.startCropping()
        assertTrue(vm.isCropping)
    }

    @Test
    fun `stopCropping clears flag`() {
        val vm = createViewModel()
        vm.startCropping()
        vm.stopCropping()
        assertFalse(vm.isCropping)
    }

    @Test
    fun `applyCrop stops cropping`() {
        val vm = createViewModel()
        vm.startCropping()
        vm.applyCrop()
        assertFalse(vm.isCropping)
    }

    @Test
    fun `goToHome navigates to Home`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.setGamma(2.0f)
        vm.rotateRight()

        vm.goToHome()
        testDispatcher.scheduler.advanceUntilIdle()
        runCurrent()

        assertTrue(vm.currentScreen is AppScreen.Home)
    }
}
