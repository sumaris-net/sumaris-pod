#!/bin/bash
# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  cd ..
  PROJECT_DIR=`pwd`
  export PROJECT_DIR
fi;

# Preparing Android environment
. ${PROJECT_DIR}/scripts/env-android.sh
if [[ $? -ne 0 ]]; then
  exit 1
fi

cd ${PROJECT_DIR}

# Run the build
echo "Running cordova build..."
ionic cordova build android --warning-mode=none --color --prod --release
if [[ $? -ne 0 ]]; then
  exit 1
fi

# Sign files
echo "Signing APK file..."
KEYSTORE_FILE=${PROJECT_DIR}/.local/Sumaris.keystore
KEY_ALIAS=Sumaris
KEY_PWD=
APK_DIR=${PROJECT_DIR}/platform/android/app/release
APK_UNSIGNED_FILE=${APK_DIR}/app-release.apk
APK_SIGNED_FILE=${APK_DIR}/app-release-signed.apk

if [[ ! -f "${APK_UNSIGNED_FILE}" ]]; then
  echo "APK file not found at: ${APK_UNSIGNED_FILE}"
  exit 1
fi
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ${KEYSTORE_FILE} ${APK_UNSIGNED_FILE} Sumaris

BUILD_TOOLS_DIR="${ANDROID_SDK_ROOT}/build-tools/28.*/"
cd ${BUILD_TOOLS_DIR}
./zipalign -v 4 ${APK_UNSIGNED_FILE} ${APK_SIGNED_FILE}

./apksigner verify ${APK_SIGNED_FILE}
