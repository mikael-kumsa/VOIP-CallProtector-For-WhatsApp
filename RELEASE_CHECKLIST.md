# Release Checklist - WhatsApp Call Protector v1.0

Use this checklist before releasing to production.

## Pre-Release

### Code Quality
- [x] All color references fixed (white-suc → white)
- [x] No debug code in release builds
- [x] ProGuard rules configured
- [x] Lint errors resolved
- [x] Code obfuscation enabled
- [x] Resource shrinking enabled

### Build Configuration
- [x] Version code: 1
- [x] Version name: 1.0
- [x] Application ID: com.lal.whatsappcallprotector
- [x] Min SDK: 26 (Android 8.0)
- [x] Target SDK: 34 (Android 14)
- [x] Signing configuration ready
- [x] Release build type configured

### Security
- [x] Keystore generation scripts created
- [x] .gitignore updated (keystore files excluded)
- [x] Sample gradle.properties created
- [x] No hardcoded passwords in code
- [x] Debug builds disabled for release

### Documentation
- [x] README.md created
- [x] PRODUCTION_BUILD.md created
- [x] Release checklist created
- [x] Privacy policy URL configured

## Build Process

### Keystore Setup
- [ ] Generate release keystore using script
- [ ] Create gradle.properties with credentials
- [ ] Verify keystore is NOT in version control
- [ ] Backup keystore securely

### Build Release
- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] Build release AAB: `./gradlew bundleRelease`
- [ ] Verify APK/AAB is signed
- [ ] Check APK size (should be ~5-10 MB)

### Testing
- [ ] Install release APK on test device
- [ ] Test all permissions flow
- [ ] Test service start/stop
- [ ] Test DND activation during WhatsApp call
- [ ] Test DND deactivation after call
- [ ] Test statistics tracking
- [ ] Test Settings screen
- [ ] Test About screen
- [ ] Test on Android 8.0 (API 26)
- [ ] Test on Android 14 (API 34)
- [ ] Test with WhatsApp
- [ ] Test with WhatsApp Business
- [ ] Verify no crashes
- [ ] Check battery usage

## Google Play Store

### Store Listing
- [ ] App name: "WhatsApp Call Protector"
- [ ] Short description (80 chars max)
- [ ] Full description (4000 chars max)
- [ ] App icon (512x512)
- [ ] Feature graphic (1024x500)
- [ ] Screenshots (at least 2)
- [ ] Privacy policy URL
- [ ] Content rating completed

### App Information
- [ ] Package name: com.lal.whatsappcallprotector
- [ ] Version: 1.0 (1)
- [ ] Category: Tools / Productivity
- [ ] Content rating: Everyone / Teen

### Permissions Declaration
- [ ] Accessibility Service explained
- [ ] DND permission explained
- [ ] Microphone permission explained
- [ ] Phone state permission explained (if used)

### Release
- [ ] Upload AAB to Play Console
- [ ] Complete store listing
- [ ] Set up pricing (Free)
- [ ] Select countries for release
- [ ] Review and publish

## Post-Release

### Monitoring
- [ ] Monitor crash reports
- [ ] Monitor user reviews
- [ ] Monitor analytics (if enabled)
- [ ] Check Play Console for issues

### Support
- [ ] Monitor support channels
- [ ] Respond to user feedback
- [ ] Address critical bugs quickly

## Version History

### v1.0 (Initial Release)
- Initial release
- Automatic DND during WhatsApp calls
- WhatsApp Business support
- Call statistics
- Material 3 UI
- Dark theme support

---

**Release Date:** _______________
**Released By:** _______________
**Notes:** _______________

