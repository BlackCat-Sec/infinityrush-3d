# Relic Rush

Relic Rush is a polished 2.5D endless runner built with Kotlin, `SurfaceView`, and the Android Canvas API. It is designed as a modern temple-and-jungle adventure runner with lane switching, missions, daily rewards, character unlocks, power-ups, and Play Store integration hooks.

## Features

- 3-lane endless runner with swipe controls
- Swipe up to jump
- Swipe down to slide
- Swipe left and right to change lanes
- 2.5D fake-depth rendering using scaling, layering, shadows, and parallax
- Dynamic temple adventure themes:
  - jungle
  - ruins
  - bridge
- Day and night color cycling during the run
- Unlockable characters with different speed and jump attributes
- Coins, mission progression, daily rewards, and lifetime leveling
- Power-ups:
  - coin magnet
  - shield
  - speed boost
  - double score
- Obstacle variety:
  - rocks
  - spikes
  - rolling boulders
  - swinging traps
  - gaps
  - low branches
- Pause, restart, revive, and home flow
- High score, lifetime coins, unlocks, and settings persisted in `SharedPreferences`
- AdMob integration hooks with official Google test IDs for:
  - rewarded revive ads
  - interstitial game-over ads
- Google Play Billing integration hooks for:
  - remove ads
  - coin pack

## Tech Stack

- Kotlin
- Android Canvas + `SurfaceView`
- Minimum SDK 24
- Target SDK 36
- Package name: `com.relicrush.game`

## Project Structure

- `app/src/main/java/com/relicrush/game/MainActivity.kt`
  - Activity host and immersive setup
- `app/src/main/java/com/relicrush/game/RelicRushApplication.kt`
  - One-time AdMob SDK initialization
- `app/src/main/java/com/relicrush/game/engine/`
  - Game loop and gameplay rules
- `app/src/main/java/com/relicrush/game/entities/`
  - Player, obstacles, coins, power-ups, missions, character data
- `app/src/main/java/com/relicrush/game/ui/`
  - Canvas layout and rendering system
- `app/src/main/java/com/relicrush/game/utils/`
  - Constants, math helpers, audio, preferences, pooling
- `app/src/main/java/com/relicrush/game/monetization/`
  - AdMob and Play Billing managers

## Open In Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select the project folder: `infinityrush`.
4. Let Gradle sync complete.
5. Use JDK 17 if Android Studio asks.

## Run The Game

1. Connect an Android device or start an emulator.
2. Press **Run** in Android Studio.
3. Start from the home screen and use swipe gestures in gameplay.

## Build Outputs

Debug APK:
- `app/build/outputs/apk/debug/app-debug.apk`

Release APK:
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## Generate A Signed Release

1. Open Android Studio.
2. Choose **Build > Generate Signed Bundle / APK**.
3. Select **Android App Bundle** for Play Store release.
4. Create or choose a keystore.
5. Complete the signing flow.

## AdMob Setup

The project currently uses Google sample AdMob IDs so it can build and be tested safely.

Before publishing:

1. Create a real AdMob app.
2. Replace the app ID in:
   - `app/src/main/res/values/strings.xml`
3. Replace the test ad unit IDs in:
   - `app/src/main/java/com/relicrush/game/utils/GameConstants.kt`

## Google Play Billing Setup

The project includes a Billing Client flow for two in-app products:

- `remove_ads`
- `coin_pack`

Before publishing:

1. Create matching in-app products in Play Console.
2. Use the same product IDs or update the constants in:
   - `app/src/main/java/com/relicrush/game/utils/GameConstants.kt`
3. Add real purchase verification on your backend for production-grade security.

## Screenshot Placeholders

Replace these with final screenshots before publishing:

- Home Screen Screenshot Placeholder
- Character Selection Screenshot Placeholder
- Gameplay Screenshot Placeholder
- Game Over Screenshot Placeholder

## Notes For Publishing

- Replace sample AdMob IDs with live IDs
- Connect real Play Console billing products
- Add a privacy policy if you keep ads or billing enabled
- Verify store listing art, screenshots, and content rating
- Increase `versionCode` and `versionName` in `app/build.gradle.kts`

## Local Verification

The project has been verified with:

- `assembleDebug`
- `assembleRelease`
