# AudioOverlay Mix

An Android overlay app for real-time audio volume management during gaming and multitasking.

This app provides a floating overlay with controls for music, chat, and system volumes, intelligent presets, and app-based auto-adjustment.

## Features

- Floating draggable button with edge anchoring
- Overlay UI with 3 volume sliders (Music, Chat, System)
- 3 default presets: Solo Game, Multi Chat, Music Priority
- Intelligent app detection for automatic preset application
- Room database for presets and action logs
- Material You themes with dark/light mode support
- Toast notifications for volume changes

## Requirements

- Android 8.0+ (API 26)
- Permissions: Overlay, Audio Settings, Usage Stats

## Setup

1. Clone the repository
2. Open in Android Studio (Giraffe+)
3. Build and run on device/emulator
4. Grant permissions when prompted

## Usage

1. Launch the app and grant permissions
2. Floating button appears on screen
3. Tap to show overlay with volume controls
4. Drag button to reposition (snaps to edges)
5. Use sliders or presets for quick adjustments
6. App detects foreground apps and applies matching presets

## Development

Built with Kotlin, Room, Coroutines. Follows Material Design 3.

First, download Android Studio from the official site:

[https://developer.android.com/studio](https://developer.android.com/studio)

## Cahier des Charges

See [cahierdecharge.md](cahierdecharge.md) for detailed requirements and implementation phases.

## Contributing

This app is built following the specifications in cahierdecharge.md. Contributions welcome for additional features like auto-duck, undo, or customization.
