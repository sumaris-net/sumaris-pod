#!/bin/bash
# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  cd ..
  PROJECT_DIR=`pwd`
  export PROJECT_DIR
fi;

# Preparing Android environment
. ${PROJECT_DIR}/scripts/env-android.sh
[[ $? -ne 0 ]] && exit 1

cd ${PROJECT_DIR}

# Run the build
echo "Running cordova build..."
ionic cordova build android --warning-mode=none --color --prod --release

if [[ $? -ne 0 ]]; then
  echo "Something's wrong with your environment. Please check if you have permissions on ~/.android"
  exit 1
fi

# Signature
KEYSTORE_FILE=${PROJECT_DIR}/.local/android/Sumaris.keystore
KEY_ALIAS=Sumaris
KEY_PWD=
APK_DIR=${PROJECT_DIR}/platforms/android/app/build/outputs/apk/release
APK_UNSIGNED_FILE=${APK_DIR}/app-release-unsigned.apk
BUILD_TOOLS_DIR="${ANDROID_SDK_ROOT}/build-tools/${ANDROID_SDK_VERSION}/"

if [[ ! -f "${APK_UNSIGNED_FILE}" ]]; then
  echo "APK file not found at: ${APK_UNSIGNED_FILE}"
  exit 1
fi

# Check if signed
cd ${BUILD_TOOLS_DIR}
./apksigner verify ${APK_UNSIGNED_FILE}

# Not signed ? Do it !
if [[ $? -ne 0 ]]; then
  echo "It seems that the APK file ${APK_UNSIGNED_FILE} is not signed !"
  if [[ ! -f "${KEYSTORE_FILE}" ]]; then
    echo "ERROR: Unable to sign: no keystore file found at ${KEYSTORE_FILE} !"
    exit 1
  fi

  echo "Signing APK file ${APK_UNSIGNED_FILE}..."
  APK_SIGNED_FILE=${APK_DIR}/app-release-signed.apk

  jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ${KEYSTORE_FILE} ${APK_UNSIGNED_FILE} Sumaris

  BUILD_TOOLS_DIR="${ANDROID_SDK_ROOT}/build-tools/28.*/"
  cd ${BUILD_TOOLS_DIR}
  ./zipalign -v 4 ${APK_UNSIGNED_FILE} ${APK_SIGNED_FILE}

  ./apksigner verify ${APK_SIGNED_FILE}
  if [[ $? -ne 0 ]]; then
    echo "Signing failed !"
    exit 1
  fi

  # Do file replacement
  rm ${APK_UNSIGNED_FILE}
  mv ${APK_SIGNED_FILE} ${APK_UNSIGNED_FILE}
fi
