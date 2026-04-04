# Infinity Rush 3D

Infinity Rush 3D is a standalone Android endless runner built with Kotlin and OpenGL ES 2.0. It keeps the easy-to-open Android Studio setup from the original project, but upgrades the gameplay into a lane-switching 3D chase runner with jump, slide, coins, fog, lighting, and a forward camera inspired by arcade runners.

## Highlights

- Native Android project with Kotlin and `GLSurfaceView`
- Three-lane 3D endless runner with swipe lane switching
- Tap to jump and swipe down to slide under gates
- Procedural obstacle patterns with blockers, hurdles, and slide gates
- Coin pickup rows, speed ramping, and persistent high score
- Start, pause, resume, and game-over overlays
- Background music and sound effects with saved audio toggles
- Separate application ID from the 2D game: `com.infinityrush.game3d`

## Tech Stack

- Kotlin
- OpenGL ES 2.0 via `GLSurfaceView.Renderer`
- Min SDK 24
- Target SDK 36

## Open In Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select the `infinityrush-3d` folder.
4. Let Gradle sync.
5. Use JDK 17 and Android SDK 36 if Android Studio asks.

## Run

1. Connect a device or start an emulator.
2. Click **Run 'app'**.
3. Use the menu overlay to start a run.

## Controls

- Swipe left: move left lane
- Swipe right: move right lane
- Tap: jump
- Swipe down: slide
- Pause button: pause the run

## Build Outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Publishing Notes

- The project uses a unique package name, so it installs separately from the original 2D Infinity Rush.
- Add your signing config and Play Store assets before publishing.
- Replace the launcher icon if you want a final branded asset.
