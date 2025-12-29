# MyPlants

MyPlants is a modern Android app to manage your plant collection, stay on top of watering schedules,
and connect to BLE plant sensors to view live readings.

## For everyone (quick overview)

### What you can do

- **Add and manage plants**: photo, name, size, water amount, description.
- **Watering schedule**: see upcoming plants, missed waterings, and history.
- **Plant details**: quickly mark a plant as watered.
- **BLE sensor support**: connect to plant sensors and see readings (temperature, moisture, light,
  conductivity).
- **Multi-language UI**: English, German (Deutsch), Romanian (Română) with in-app language
  switching.

### App preview (add your media here)

Add screenshots/GIFs/videos to make the project instantly understandable.

- **Screen recording (recommended)**: `[ADD_LINK_HERE]`
- **Short demo GIF**: `docs/media/demo.gif`

#### Screenshots

| Plant list                  | Plant details                  | Add/Edit plant            | Settings                  |
|-----------------------------|--------------------------------|---------------------------|---------------------------|
| `docs/media/plant_list.png` | `docs/media/plant_details.png` | `docs/media/add_edit.png` | `docs/media/settings.png` |

> Tip: create a `docs/media/` folder and drop your images/recordings there.

## For developers

### Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: single-activity + Compose navigation
- **DI**: Hilt
- **Background work**: WorkManager
- **Image loading**: Coil
- **BLE**: Nordic Semiconductor BLE library
- **Localization**: Android resources (`values`, `values-de`, `values-ro`) + AppCompat per-app
  locales

### Requirements

- **Android Studio**: latest stable recommended
- **JDK**: 17
- **Android SDK**: `compileSdk = 36` (project is configured for it)

### How to run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on an emulator or device.

Or from terminal:

```bash
./gradlew :app:assembleDebug
```

### Permissions

- **Camera**: used for taking plant photos.
- **Notifications**: used for watering reminders.
- **Bluetooth**: used for sensor connection.
- **Storage (older Android)**: used for picking images from gallery.

### Localization

Supported languages:

- English (`en`)
- German (`de`)
- Romanian (`ro`)

Language can be changed from **Settings → App Language**.


