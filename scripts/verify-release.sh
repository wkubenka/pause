#!/usr/bin/env bash
# Builds a release APK the way CI does, using a throwaway keystore, and checks
# the properties Astute Repo depends on: signature, versionName, versionCode, package.
set -euo pipefail
cd "$(dirname "$0")/.."

SDK_DIR=$(grep '^sdk.dir=' local.properties | cut -d= -f2)
BUILD_TOOLS=$(ls -d "$SDK_DIR"/build-tools/* | sort -V | tail -1)
WORK=$(mktemp -d)
KS="$WORK/test.jks"

keytool -genkeypair -keystore "$KS" -storepass testpass -keypass testpass \
  -alias test -keyalg RSA -keysize 2048 -validity 1 -dname "CN=Test" >/dev/null 2>&1

KEYSTORE_FILE="$KS" KEYSTORE_PASSWORD=testpass KEY_ALIAS=test KEY_PASSWORD=testpass \
  ./gradlew -q assembleRelease -PVERSION_NAME=1.2.3 -PVERSION_CODE=10203

APK=app/build/outputs/apk/release/app-release.apk
test -f "$APK" || { echo "missing $APK"; exit 1; }
"$BUILD_TOOLS/apksigner" verify "$APK"
BADGING=$("$BUILD_TOOLS/aapt2" dump badging "$APK" | sed -n 1p)
echo "$BADGING"
grep -q "name='com.astute.pause'" <<<"$BADGING"
grep -q "versionCode='10203'" <<<"$BADGING"
grep -q "versionName='1.2.3'" <<<"$BADGING"
PERMS=$("$BUILD_TOOLS/aapt2" dump permissions "$APK")
if grep -q 'android.permission.INTERNET' <<<"$PERMS"; then
  echo "INTERNET permission must not be present"; exit 1
fi
MANIFEST=$("$BUILD_TOOLS/aapt2" dump xmltree --file AndroidManifest.xml "$APK")
if ! grep -q 'allowBackup.*=false' <<<"$MANIFEST"; then
  echo "allowBackup must be false (purchase data stays off Google)"; exit 1
fi
if ! grep -q 'dataExtractionRules' <<<"$MANIFEST"; then
  echo "dataExtractionRules must be set (excludes Android 12+ cloud backup and device transfer)"; exit 1
fi
echo "OK: release APK is Astute Repo compatible"
