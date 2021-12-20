#!/bin/bash

# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  PROJECT_DIR=$(cd ${SCRIPT_DIR}/.. && pwd)
  export PROJECT_DIR
fi;

# Preparing Android environment
. ${PROJECT_DIR}/scripts/env-android.sh
[[ $? -ne 0 ]] && exit 1

# Clean all previous APK
if [[ -d "${ANDROID_OUTPUT_APK_RELEASE}" ]]; then
  rm ${ANDROID_OUTPUT_APK_RELEASE}/*.apk
fi
if [[ -d "${ANDROID_OUTPUT_APK_DEBUG}" ]]; then
  rm ${ANDROID_OUTPUT_APK_DEBUG}/*.apk
fi

# Run the build
echo "Building Android application..."
cd ${PROJECT_DIR}
node ${NODE_OPTIONS} ./node_modules/@ionic/cli/bin/ionic cordova build android --warning-mode=none --color $*
[[ $? -ne 0 ]] && exit 1

echo "Running Android application..."
if [[ -f "${ANDROID_OUTPUT_APK_RELEASE}/${ANDROID_OUTPUT_APK_PREFIX}-release.apk" ]]; then
  native-run android --app ${ANDROID_OUTPUT_APK_RELEASE}/${ANDROID_OUTPUT_APK_PREFIX}-release.apk
elif [[ -f "${ANDROID_OUTPUT_APK_RELEASE}/${ANDROID_OUTPUT_APK_PREFIX}-release-unsigned.apk" ]]; then
  native-run android --app ${ANDROID_OUTPUT_APK_RELEASE}/${ANDROID_OUTPUT_APK_PREFIX}-release-unsigned.apk
else
  native-run android --app ${ANDROID_OUTPUT_APK_DEBUG}/${ANDROID_OUTPUT_APK_PREFIX}-debug.apk
fi

