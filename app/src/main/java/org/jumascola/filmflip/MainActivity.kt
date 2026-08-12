package org.jumascola.filmflip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jumascola.filmflip.ui.screens.BacklightScreen
import org.jumascola.filmflip.ui.screens.CameraScreen
import org.jumascola.filmflip.ui.screens.EditScreen
import org.jumascola.filmflip.ui.screens.HomeScreen
import org.jumascola.filmflip.ui.theme.FilmFlipTheme
import org.jumascola.filmflip.viewmodel.AppScreen
import org.jumascola.filmflip.viewmodel.FilmFlipViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FilmFlipViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilmFlipTheme {
                val context = androidx.compose.ui.platform.LocalContext.current

                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                    onResult = { uri ->
                        uri?.let {
                            viewModel.loadFromGallery(context, it)
                        }
                    }
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (viewModel.currentScreen) {
                        is AppScreen.Home -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onGalleryClick = {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                        }
                        is AppScreen.Camera -> {
                            CameraScreen(viewModel = viewModel)
                        }
                        is AppScreen.Edit -> {
                            EditScreen(viewModel = viewModel)
                        }
                        is AppScreen.Backlight -> {
                            BacklightScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
