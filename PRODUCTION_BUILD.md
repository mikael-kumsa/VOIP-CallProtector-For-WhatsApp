# Production Build Guide - WhatsApp Call Protector v1.0

This guide will help you build a production-ready release APK/AAB for WhatsApp Call Protector.

## Prerequisites

1. **Android Studio** (latest stable version)
2. **JDK 8 or higher**
3. **Android SDK** with API level 34
4. **Gradle** (included with Android Studio)

## Step 1: Generate Release Keystore

### Option A: Using the provided script (Recommended)

**Windows:**
```bash
generate-keystore.bat
```

**Linux/Mac:**
```bash
chmod +x generate-keystore.sh
./generate-keystore.sh
```

The script will:
- Generate a keystore file at `app/release.keystore`
- Prompt you for passwords
- Provide the configuration to add to `gradle.properties`

### Option B: Manual generation

```bash
keytool -genkey -v -keystore app/release.keystore -alias whatsapp-call-protector -keyalg RSA -keysize 2048 -validity 9125
```

**Important:** 
- Use a strong password (at least 12 characters recommended)
- Store the keystore and passwords securely
- **NEVER commit the keystore to version control**

## Step 2: Configure Signing Credentials

Create or edit `gradle.properties` in the project root (not in the app folder):

```properties
# Release Keystore Configuration
KEYSTORE_FILE=app/release.keystore
KEYSTORE_PASSWORD=your_keystore_password_here
KEY_ALIAS=whatsapp-call-protector
KEY_PASSWORD=your_key_password_here
```

**Security Note:** 
- Add `gradle.properties` to `.gitignore` if it contains sensitive information
- For CI/CD, use environment variables instead

## Step 3: Verify Build Configuration

The app is already configured for production:

- ✅ **Version:** 1.0.0 (versionCode: 1)
- ✅ **Application ID:** com.lal.whatsappcallprotector
- ✅ **Min SDK:** 26 (Android 8.0)
- ✅ **Target SDK:** 34 (Android 14)
- ✅ **ProGuard:** Enabled for release builds
- ✅ **Code Shrinking:** Enabled
- ✅ **Resource Shrinking:** Enabled
- ✅ **Debugging:** Disabled for release

## Step 4: Build Release APK

### Using Android Studio:

1. Open the project in Android Studio
2. Go to **Build** → **Generate Signed Bundle / APK**
3. Select **APK** or **Android App Bundle (AAB)** (recommended for Play Store)
4. Select your keystore file (`app/release.keystore`)
5. Enter keystore password and key alias/password
6. Select **release** build variant
7. Click **Finish**

The signed APK/AAB will be generated in `app/release/`

### Using Command Line:

**Build APK:**
```bash
./gradlew assembleRelease
```

**Build AAB (for Play Store):**
```bash
./gradlew bundleRelease
```

**Windows:**
```bash
gradlew.bat assembleRelease
gradlew.bat bundleRelease
```

Output location:
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## Step 5: Verify the Build

Before distributing:

1. **Test the APK:**
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

2. **Verify signing:**
   ```bash
   jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
   ```

3. **Check APK info:**
   ```bash
   aapt dump badging app/build/outputs/apk/release/app-release.apk
   ```

## Step 6: Prepare for Google Play Store

### Required Information:

1. **App Name:** WhatsApp Call Protector
2. **Package Name:** com.lal.whatsappcallprotector
3. **Version:** 1.0.0 (1)
4. **Privacy Policy URL:** https://whatsappcallprotector.michaelkumsa.com/privacy-policy

### Store Listing Requirements:

- ✅ App icon (512x512 and 1024x1024)
- ✅ Feature graphic (1024x500)
- ✅ Screenshots (at least 2, up to 8)
- ✅ Short description (80 characters max)
- ✅ Full description (4000 characters max)
- ✅ Privacy policy URL
- ✅ Content rating questionnaire

### Content Rating:

The app requires:
- Accessibility Service permission
- Do Not Disturb permission
- Microphone permission (for call detection)

These are standard permissions for utility apps.

## Step 7: Testing Checklist

Before releasing, test:

- [ ] App installs correctly
- [ ] All permissions can be granted
- [ ] Service starts and stops correctly
- [ ] DND activates during WhatsApp calls
- [ ] DND deactivates after calls end
- [ ] Statistics tracking works
- [ ] Settings screen functions properly
- [ ] About screen displays correctly
- [ ] App works on Android 8.0+ (API 26+)
- [ ] App works with both WhatsApp and WhatsApp Business
- [ ] No crashes or ANRs
- [ ] Battery usage is reasonable

## Troubleshooting

### Build fails with "keystore not found"

- Ensure `gradle.properties` exists in the project root
- Verify the `KEYSTORE_FILE` path is correct
- Check that the keystore file exists at the specified path

### Build fails with "signing config not found"

- Ensure all signing credentials are set in `gradle.properties`
- Check that keystore password and key password are correct

### APK is too large

- The app uses ProGuard/R8 for code shrinking
- Resource shrinking is enabled
- Current size should be ~5-10 MB

### ProGuard warnings

- Most warnings are safe to ignore
- Check `app/proguard-rules.pro` for custom rules
- Test the release build thoroughly

## Version Management

For future releases:

1. Update `versionCode` in `app/build.gradle.kts` (increment by 1)
2. Update `versionName` in `app/build.gradle.kts` (e.g., "1.0.1")
3. Update version in About screen (auto-detected from package)

## Security Best Practices

1. **Never commit:**
   - `release.keystore` file
   - `gradle.properties` with passwords
   - Any signing credentials

2. **Backup:**
   - Store keystore in secure location (encrypted)
   - Keep multiple backups
   - Document keystore passwords securely

3. **CI/CD:**
   - Use environment variables for credentials
   - Never log passwords
   - Use secure secret management

## Support

For issues or questions:
- Author: Michael Kumsa
- Privacy Policy: https://whatsappcallprotector.michaelkumsa.com/privacy-policy

---

**Good luck with your release! 🚀**

