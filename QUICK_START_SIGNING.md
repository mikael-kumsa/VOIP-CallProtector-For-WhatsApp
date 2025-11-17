# Quick Start: Signing Your APK

Your APK was built but is **unsigned**. Follow these steps to sign it:

## Option 1: Generate Keystore and Rebuild (Recommended)

### Step 1: Generate Keystore

**Windows:**
```powershell
.\generate-keystore.bat
```

The script will:
- Create `app/release.keystore`
- Ask for passwords
- Show you what to add to `gradle.properties`

### Step 2: Create gradle.properties

Create `gradle.properties` in the project root (same folder as `build.gradle.kts`):

```properties
KEYSTORE_FILE=app/release.keystore
KEYSTORE_PASSWORD=your_keystore_password_here
KEY_ALIAS=whatsapp-call-protector
KEY_PASSWORD=your_key_password_here
```

**Important:** Replace the passwords with the ones you used when generating the keystore.

### Step 3: Rebuild Release APK

```powershell
.\gradlew clean assembleRelease
```

The new APK will be automatically signed and located at:
`app\build\outputs\apk\release\app-release.apk`

### Step 4: Verify Signature

```powershell
jarsigner -verify -verbose -certs app\build\outputs\apk\release\app-release.apk
```

You should see "jar verified" instead of "jar is unsigned".

---

## Option 2: Manually Sign Existing APK

If you already have a keystore and want to sign the existing APK:

### Step 1: Use the Signing Script

```powershell
.\sign-apk.ps1 -ApkPath "app\release\app-release.apk" -KeystorePath "app\release.keystore" -Alias "whatsapp-call-protector"
```

The script will:
- Align the APK
- Sign it with your keystore
- Verify the signature
- Create `app-release-signed.apk`

### Step 2: Replace Original APK

```powershell
Move-Item -Force app\release\app-release-signed.apk app\release\app-release.apk
```

---

## Option 3: Manual Signing (Advanced)

If you prefer to sign manually:

### Step 1: Align APK

```powershell
$env:ANDROID_HOME\build-tools\34.0.0\zipalign.exe -v -p 4 app\release\app-release.apk app\release\app-release-aligned.apk
```

### Step 2: Sign APK

```powershell
$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat sign --ks app\release.keystore --ks-key-alias whatsapp-call-protector --out app\release\app-release-signed.apk app\release\app-release-aligned.apk
```

You'll be prompted for passwords.

### Step 3: Verify

```powershell
$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat verify --verbose app\release\app-release-signed.apk
```

---

## Troubleshooting

### "Keystore file not found"
- Run `generate-keystore.bat` first to create the keystore

### "zipalign not found" or "apksigner not found"
- Set `ANDROID_HOME` environment variable to your Android SDK path
- Or install Android SDK Build Tools 34.0.0
- Or use Android Studio's built-in tools

### "jarsigner: unable to sign jar"
- Make sure you're using `apksigner` (not `jarsigner`) for APK signing
- `jarsigner` is for JAR files, `apksigner` is for APK files

### "Keystore was tampered with, or password was incorrect"
- Double-check your keystore password
- Make sure you're using the correct key alias

---

## Next Steps

After signing:
1. ✅ Test the signed APK on a device
2. ✅ Verify it installs correctly
3. ✅ Test all functionality
4. ✅ Upload to Google Play Store (if using AAB, build with `.\gradlew bundleRelease`)

---

**Need Help?** See `PRODUCTION_BUILD.md` for detailed instructions.

