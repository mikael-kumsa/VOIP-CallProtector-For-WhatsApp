# VOIP Call Protector

Automatically enables Do Not Disturb mode during messaging app calls to prevent interruptions from incoming calls and notifications.

**Disclaimer:** This app works with third-party messaging applications but is not affiliated with, endorsed by, or sponsored by any messaging service provider. All trademarks and registered trademarks are the property of their respective owners.

## Features

- ✅ **Automatic DND Activation** - Enables Do Not Disturb during messaging app calls
- ✅ **Multi-App Support** - Works with popular messaging applications
- ✅ **Call Statistics** - Track total calls and call duration
- ✅ **Privacy-Focused** - All processing happens locally on your device
- ✅ **No Data Collection** - No information is collected or shared
- ✅ **Material 3 Design** - Modern, beautiful UI with dark theme support

## Requirements

- Android 8.0 (API 26) or higher
- A messaging application installed that supports VOIP calls
- Required permissions:
  - Accessibility Service (to detect messaging app calls)
  - Do Not Disturb access (to manage DND mode)
  - Microphone permission (to distinguish calls from voice messages)

## Installation

### From Google Play Store
Coming soon...

### Manual Installation
1. Download the latest APK from the [Releases](../../releases) page
2. Enable "Install from Unknown Sources" in your device settings
3. Install the APK
4. Grant required permissions when prompted

## Usage

1. **Grant Permissions:**
   - Open the app
   - Tap "Grant Permissions"
   - Follow the wizard to grant all required permissions

2. **Start Protection:**
   - Tap "Start Protection" button
   - The service will run in the background

3. **During Messaging App Calls:**
   - DND will automatically activate when a call starts
   - DND will automatically deactivate when the call ends

4. **View Statistics:**
   - Go to Settings (menu → Settings)
   - View total calls and call duration

## Building from Source

See [PRODUCTION_BUILD.md](PRODUCTION_BUILD.md) for detailed build instructions.

### Quick Build

1. Clone the repository
2. Open in Android Studio
3. Generate keystore: `./generate-keystore.sh` (or `.bat` on Windows)
4. Configure `gradle.properties` (see `gradle.properties.example`)
5. Build release: `./gradlew assembleRelease`

## Privacy

This app processes all data locally on your device. No information is collected, stored on external servers, or shared with third parties.

**Privacy Policy:** https://whatsappcallprotector.michaelkumsa.com/privacy-policy

## Permissions Explained

- **Accessibility Service:** Required to detect when messaging apps are in a call by monitoring the screen
- **Do Not Disturb Access:** Required to automatically enable/disable DND mode
- **Microphone Permission:** Used to detect when the microphone is active to distinguish between calls and voice messages
- **Phone State:** Used to detect incoming calls (optional, for future features)

## Troubleshooting

### DND doesn't activate during calls
- Ensure all permissions are granted
- Check that the service is running (status should show "Protection Active")
- Try restarting the service

### Service stops unexpectedly
- Check battery optimization settings (disable optimization for this app)
- Ensure the app is not being killed by the system

### App doesn't detect messaging app calls
- Ensure Accessibility Service is enabled for "VOIP Call Detector"
- Try restarting the app
- Check that your messaging app is up to date

## Support

For issues, questions, or feature requests, please contact:
- **Author:** Michael Kumsa
- **Privacy Policy:** https://whatsappcallprotector.michaelkumsa.com/privacy-policy

## License

Copyright © 2024 Michael Kumsa. All rights reserved.

## Version

**Current Version:** 1.0 (Build 1)

---

**Made with ❤️ by Michael Kumsa**

