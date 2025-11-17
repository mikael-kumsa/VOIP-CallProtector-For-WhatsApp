@echo off
REM WhatsApp Call Protector - Keystore Generation Script (Windows)
REM This script generates a release keystore for signing the app

echo ==========================================
echo WhatsApp Call Protector - Keystore Generator
echo ==========================================
echo.

REM Configuration
set KEYSTORE_NAME=release.keystore
set KEYSTORE_PATH=app\%KEYSTORE_NAME%
set KEY_ALIAS=voip-call-protector
set VALIDITY_YEARS=25

REM Check if keystore already exists
if exist "%KEYSTORE_PATH%" (
    echo WARNING: Keystore already exists at %KEYSTORE_PATH%
    set /p overwrite="Do you want to overwrite it? (yes/no): "
    if /i not "%overwrite%"=="yes" (
        echo Aborted.
        exit /b 1
    )
    del "%KEYSTORE_PATH%"
)

REM Prompt for passwords
echo Enter keystore password (min 6 characters):
set /p KEYSTORE_PASSWORD=

if "%KEYSTORE_PASSWORD%"=="" (
    echo ERROR: Password cannot be empty
    exit /b 1
)

echo Re-enter keystore password:
set /p KEYSTORE_PASSWORD_CONFIRM=

if not "%KEYSTORE_PASSWORD%"=="%KEYSTORE_PASSWORD_CONFIRM%" (
    echo ERROR: Passwords do not match
    exit /b 1
)

echo Enter key password (or press Enter to use same as keystore password):
set /p KEY_PASSWORD=

if "%KEY_PASSWORD%"=="" (
    set KEY_PASSWORD=%KEYSTORE_PASSWORD%
)

REM Generate keystore
echo Generating keystore...
keytool -genkey -v ^
    -keystore "%KEYSTORE_PATH%" ^
    -alias "%KEY_ALIAS%" ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity %VALIDITY_YEARS%00 ^
    -storepass "%KEYSTORE_PASSWORD%" ^
    -keypass "%KEY_PASSWORD%" ^
    -dname "CN=VOIP Call Protector, OU=Development, O=Michael Kumsa, L=Unknown, ST=Unknown, C=US"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo SUCCESS: Keystore generated successfully!
    echo ==========================================
    echo.
    echo Keystore location: %KEYSTORE_PATH%
    echo Key alias: %KEY_ALIAS%
    echo Validity: %VALIDITY_YEARS% years
    echo.
    echo IMPORTANT: Add these to your gradle.properties file:
    echo KEYSTORE_FILE=%KEYSTORE_PATH%
    echo KEYSTORE_PASSWORD=%KEYSTORE_PASSWORD%
    echo KEY_ALIAS=%KEY_ALIAS%
    echo KEY_PASSWORD=%KEY_PASSWORD%
    echo.
    echo Make sure gradle.properties is in .gitignore!
    echo.
) else (
    echo.
    echo ERROR: Failed to generate keystore
    exit /b 1
)

pause

