#!/bin/bash

#export ANDROID_NDK_VERSION=r19c
#export ANDROID_SDK_VERSION=r29.0.2
#export ANDROID_SDK_TOOLS_VERSION=4333796

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
#export ANDROID_SDK_ROOT=/usr/lib/android-sdk
#export ANDROID_SDK_TOOLS_ROOT=${ANDROID_SDK_ROOT}/build-tools

#export PATH=${ANDROID_SDK_TOOLS_ROOT}/tools/bin:$PATH

# Mozilla developer account
export AMO_JWT_ISSUER="user:15721294:977"
export AMO_JWT_SECRET="acbe05020fcd6df0189c24a73e195cd29aa90da6f09fb23c019da55e1fe97588"

#export WEB_EXT_ID="{...}"

export PATH=${JAVA_HOME}/bin:$PATH
