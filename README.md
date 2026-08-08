# FilmFlip

An Android app that restores photos from film negatives. Shoot a negative with the camera or pick a photo from the gallery — FilmFlip automatically inverts colors and lets you fine-tune the result.

## Features

- **Camera capture** — built-in preview for shooting negatives
- **Gallery import** — load negative photos from your gallery
- **Auto white balance** — brightest pixel detection and channel correction
- **Negative inversion** — converts negatives to positives
- **Adjustable parameters** — gamma, contrast, brightness, color warmth
- **Image rotation** — 90° left/right rotation
- **Interactive cropping** — drag handles to crop the image
- **Backlight screen** — white screen for illuminating film
- **Save to gallery** — export the processed result

## Screenshots

_Home → Camera/Gallery → Editor → Result_

## Tech Stack

- **Kotlin** — primary language
- **Jetpack Compose** — UI framework
- **CameraX** — camera integration
- **ViewModel** — state management
- **Kotlin Coroutines** — async operations

## Architecture

```
app/src/main/java/com/example/filmflip/
├── MainActivity.kt              # Entry point, screen routing
├── processor/
│   ├── NegativeProcessor.kt     # Pixel processing (inversion, WB, gamma, crop)
│   └── ProcessingParams.kt      # Processing parameters
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt         # Home screen
│   │   ├── CameraScreen.kt       # Camera
│   │   ├── EditScreen.kt         # Editor with cropping
│   │   └── BacklightScreen.kt    # Backlight
│   ├── crop/
│   │   └── CropLogic.kt          # Crop logic (pure functions)
│   └── theme/
│       └── Theme.kt              # App theme
└── viewmodel/
    └── FilmFlipViewModel.kt      # App state management
```

## Processing Pipeline

1. **Auto white balance** — find brightest pixel, scale channels
2. **Inversion** — `255 - pixel` for each RGB channel
3. **Gamma correction** — `pow(pixel / 255, 1 / gamma) * 255`
4. **Contrast & brightness** — linear transformation
5. **Color warmth** — shift red and blue channels
6. **Rotation** — Canvas transform (90°, 180°, 270°)
7. **Cropping** — rectangular bitmap crop

## Installation

### Requirements

- Android Studio Ladybug (2024.2) or newer
- Android SDK 36
- JDK 11 or newer
- Android device or emulator (API 28+)

### Build

```bash
git clone https://github.com/Jumas-Cola/FilmFlip.git
cd FilmFlip
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires connected device)
./gradlew connectedAndroidTest
```

## License

MIT
