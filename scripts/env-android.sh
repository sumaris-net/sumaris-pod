#!/bin/bash

# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  cd ..
  PROJECT_DIR=`pwd`
  export PROJECT_DIR
fi;

# Preparing environment
. ${PROJECT_DIR}/scripts/env-global.sh
if [[ $? -ne 0 ]]; then
  exit 1
fi

if [[ "_" == "_${CORDOVA_ANDROID_GRADLE_DISTRIBUTION_URL}" ]]; then
  echo "Missing Gradle distribution URL - please export env variable 'CORDOVA_ANDROID_GRADLE_DISTRIBUTION_URL'"
  exit 1
fi

echo "Preparing Android environment:"
echo " - using Android SDK: ${ANDROID_SDK_ROOT}"
echo " - using Gradle: ${CORDOVA_ANDROID_GRADLE_DISTRIBUTION_URL}"
echo " - using Java: ${JAVA_HOME}"
echo " - project dir: ${PROJECT_DIR}"

cd ${PROJECT_DIR}

# Prepare Android platform
if [[ ! -d "${PROJECT_DIR}/platforms/android" ]]; then
  echo "Adding Cordova Android platform..."
  ionic cordova prepare android --color --verbose
  if [[ $? -ne 0 ]]; then
    exit 1
  fi
fi

# Copy local files into the android project
echo "Copying files from directory '${PROJECT_DIR}/resources/android/build/local'"
cp -rf ${PROJECT_DIR}/resources/android/build/local/* ${PROJECT_DIR}/platforms/android
