# TTS Buttons Overlay

An Android accessibility overlay app that provides floating buttons for text-to-speech functionality with custom keyboard-like features.

## Features

- **Floating Overlay Interface**: Four easily accessible buttons that float on your screen
- **Select All & Copy**: Automatically selects and copies text from focused input fields
- **Read Aloud**: Converts selected text to speech using Android's TTS engine
- **Undo Function**: Restores previously cleared text
- **Stop TTS**: Halts text-to-speech playback
- **Force Speaker Output**: Routes TTS audio to phone's internal speaker (bypasses aux/headphones)
- **Accessibility Integration**: Uses Android Accessibility Service to interact with text fields

## Screenshots

The overlay appears as a vertical column of buttons on the right side of your screen:
- 📝 Select All
- 🔊 Read Aloud
- ↩️ Undo
- ⏹️ Stop the program
![POC.JPEG](_static/POC.JPEG)

## Requirements

- **Android 6.0+ (API Level 23+)**
- **Accessibility Service Permission**: Required to read screen text and interact with input fields
- **Overlay Permission**: Required to display floating buttons over other apps
- **Audio Modification Permission**: Required to force speaker output

## Installation

1. Clone or download this repository
2. Open in Android Studio
3. Build and install the APK on your device
4. Enable required permissions (see Setup section)

## Setup

### 1. Enable Overlay Permission
- Go to **Settings → Apps → TTS Buttons Overlay → Advanced → Display over other apps**
- Toggle **Allow display over other apps** to ON

### 2. Enable Accessibility Service
- Go to **Settings → Accessibility → Downloaded apps → TTS Buttons Overlay**
- Toggle the service to ON
- Confirm the permission dialog

### 3. Grant Audio Permission
- The app will request **MODIFY_AUDIO_SETTINGS** permission on first use

## Usage

1. **Start the Service**: Launch the app and tap "Start Overlay Service"
2. **Position**: The overlay buttons appear on the right side of your screen
3. **Basic Workflow**:
    - Focus on any text input field (tap in a text box)
    - Tap **Select All** to highlight and copy the text
    - Tap **Read Aloud** to hear the text spoken
    - Use **Undo** to restore previously cleared text
    - Use **Stop** to halt speech or close the overlay

## Button Functions

| Button | Function | Description |
|--------|----------|-------------|
| 📝 **Select All** | `performSelectAllAndCopy()` | Selects all text in focused input field and copies to clipboard |
| 🔊 **Read Aloud** | `getCurrentScreenText()` + TTS | Reads text from focused field using text-to-speech |
| ↩️ **Undo** | `performUndo()` | Restores the last cleared text to the input field |
| ⏹️ **Stop** | `stopSelf()` | Stops TTS playback and closes the overlay service |

## Technical Details

### Architecture
- **OverlayService**: Main service that creates and manages the floating UI
- **MyAccessibilityService**: Handles text extraction and field manipulation via Android Accessibility APIs
- **Foreground Service**: Runs persistently with notification for overlay functionality

### Key Components
- **WindowManager**: Creates floating overlay window
- **TextToSpeech Engine**: Converts text to speech with speaker routing
- **AccessibilityNodeInfo**: Reads and manipulates text in other apps
- **AudioManager**: Forces audio output to phone speaker

### Audio Routing
The app automatically routes TTS audio to the phone's internal speaker, bypassing aux/headphone outputs:
```kotlin
audioManager.mode = AudioManager.MODE_NORMAL
audioManager.isSpeakerphoneOn = true
// Uses USAGE_MEDIA with FLAG_AUDIBILITY_ENFORCED
```

## Permissions

### Required Permissions
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### Accessibility Service
The app registers an accessibility service to:
- Read text from input fields across all apps
- Perform select, copy, and text manipulation actions
- Access the accessibility node tree for text extraction

## Troubleshooting

### Overlay Not Appearing
- Ensure "Display over other apps" permission is granted
- Check that the overlay service is running (notification should be visible)

### No Audio Output
- Verify TTS engine is installed and configured in Android Settings
- Check that MODIFY_AUDIO_SETTINGS permission is granted
- Try manually switching to speaker mode, then use the app

### Accessibility Features Not Working
- Confirm the accessibility service is enabled in Android Settings
- Restart the app after enabling accessibility service
- Ensure you're tapping in text input fields before using buttons

### Text Not Being Read
- Make sure a text input field has focus (cursor visible)
- Try typing some text first, then use Select All → Read Aloud
- Check that the accessibility service has permission to read screen content

## Development

### Project Structure
```
app/src/main/java/com/example/buttons_tts_overlay/
├── MainActivity.kt          # Main launcher activity
├── OverlayService.kt        # Floating overlay service  
└── MyAccessibilityService.kt # Accessibility text handling

app/src/main/res/
├── layout/
│   ├── activity_main.xml    # Main activity layout
│   └── overlay_layout.xml   # Floating buttons layout
├── drawable/               # Button icons (ic_select_all, ic_read_aloud, etc.)
└── values/
    └── strings.xml         # App strings and labels
```

### Building
1. Open project in Android Studio
2. Sync Gradle files
3. Build → Generate Signed Bundle/APK
4. Install on device and complete setup steps

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built using Android Accessibility Service APIs
- Uses Android's built-in TextToSpeech engine
- Inspired by accessibility needs for text-to-speech functionality