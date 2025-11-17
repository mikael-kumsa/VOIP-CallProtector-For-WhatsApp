#!/bin/bash

# WhatsApp Call Protector - Keystore Generation Script
# This script generates a release keystore for signing the app

echo "=========================================="
echo "WhatsApp Call Protector - Keystore Generator"
echo "=========================================="
echo ""

# Configuration
KEYSTORE_NAME="release.keystore"
KEYSTORE_PATH="app/${KEYSTORE_NAME}"
KEY_ALIAS="voip-call-protector"
VALIDITY_YEARS=25

# Check if keystore already exists
if [ -f "$KEYSTORE_PATH" ]; then
    echo "WARNING: Keystore already exists at $KEYSTORE_PATH"
    read -p "Do you want to overwrite it? (yes/no): " overwrite
    if [ "$overwrite" != "yes" ]; then
        echo "Aborted."
        exit 1
    fi
    rm "$KEYSTORE_PATH"
fi

# Prompt for passwords
echo "Enter keystore password (min 6 characters):"
read -s KEYSTORE_PASSWORD
echo ""

if [ ${#KEYSTORE_PASSWORD} -lt 6 ]; then
    echo "ERROR: Password must be at least 6 characters"
    exit 1
fi

echo "Re-enter keystore password:"
read -s KEYSTORE_PASSWORD_CONFIRM
echo ""

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    echo "ERROR: Passwords do not match"
    exit 1
fi

echo "Enter key password (or press Enter to use same as keystore password):"
read -s KEY_PASSWORD
echo ""

if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD="$KEYSTORE_PASSWORD"
fi

# Generate keystore
echo "Generating keystore..."
keytool -genkey -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $((VALIDITY_YEARS * 365)) \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=WhatsApp Call Protector, OU=Development, O=Michael Kumsa, L=Unknown, ST=Unknown, C=US"

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "SUCCESS: Keystore generated successfully!"
    echo "=========================================="
    echo ""
    echo "Keystore location: $KEYSTORE_PATH"
    echo "Key alias: $KEY_ALIAS"
    echo "Validity: $VALIDITY_YEARS years"
    echo ""
    echo "IMPORTANT: Add these to your gradle.properties file:"
    echo "KEYSTORE_FILE=$KEYSTORE_PATH"
    echo "KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD"
    echo "KEY_ALIAS=$KEY_ALIAS"
    echo "KEY_PASSWORD=$KEY_PASSWORD"
    echo ""
    echo "Make sure gradle.properties is in .gitignore!"
    echo ""
else
    echo ""
    echo "ERROR: Failed to generate keystore"
    exit 1
fi

