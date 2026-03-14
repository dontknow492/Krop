# Krop

Krop is a desktop image annotation tool built with Kotlin Multiplatform and Compose Multiplatform.
The current target in this repository is JVM desktop.

## Features

- Browse images and select files/folders for annotation
- Annotate with multiple canvas tools (for example rectangle, polygon, oval, line, circle)
- Undo/redo and zoom controls
- Import/export annotations in common dataset formats (COCO, YOLO, Pascal VOC, JSON)
- Persist app/session settings between runs

## Tech Stack

- Kotlin Multiplatform (`jvm()` target currently enabled)
- Compose Multiplatform Desktop
- Koin (dependency injection)
- Kotlinx Serialization
- Coil 3 (image loading)
- FileKit (file dialogs)
- Napier (logging)

## Prerequisites

- JDK 17 or newer
- Gradle wrapper (included)
- Git (recommended; build metadata in `buildkonfig` reads git info)

## Getting Started

Clone and run from the project root:

```powershell
Set-Location "D:\Program\KMP\Krop"
.\gradlew.bat :composeApp:run
```

## Development Commands

Run tests:

```powershell
Set-Location "D:\Program\KMP\Krop"
.\gradlew.bat :composeApp:jvmTest
```

Build the desktop app:

```powershell
Set-Location "D:\Program\KMP\Krop"
.\gradlew.bat :composeApp:build
```

Create a distributable for the current OS:

```powershell
Set-Location "D:\Program\KMP\Krop"
.\gradlew.bat :composeApp:packageDistributionForCurrentOS
```

Other available packaging tasks include `:composeApp:packageMsi`, `:composeApp:packageDmg`, and `:composeApp:packageDeb`.

## Project Layout

- `composeApp/` - Desktop application module
- `composeApp/src/jvmMain/kotlin/com/ghost/krop/` - Main application source code
- `composeApp/src/jvmTest/` - JVM tests
- `gradle/libs.versions.toml` - Dependency and plugin versions

## Notes

- The project uses `buildkonfig` to capture git/build metadata at configuration time.
- If you hit configuration-cache errors related to external commands, run Gradle with configuration cache disabled for that invocation:

```powershell
Set-Location "D:\Program\KMP\Krop"
.\gradlew.bat :composeApp:run -Dorg.gradle.configuration-cache=false
```

## License

Add your preferred license information here.

