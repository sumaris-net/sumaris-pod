#!/bin/bash
# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  PROJECT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
  export PROJECT_DIR
fi;

# Preparing Android environment
. ${PROJECT_DIR}/scripts/env-android.sh
[[ $? -ne 0 ]] && exit 1

cd ${PROJECT_DIR}


### Control that the script is run on `dev` branch
branch=`git rev-parse --abbrev-ref HEAD`
if [[ ! "$branch" = "master" ]];
then
  echo ">> This script must be run under \`master\` branch"
  exit 1
fi

PROJECT_DIR=`pwd`

### Get current version (package.json)
current=`grep -oP "version\": \"\d+.\d+.\d+((a|b)[0-9]+)?" package.json | grep -m 1 -oP "\d+.\d+.\d+((a|b)[0-9]+)?"`
if [[ "_$current" == "_" ]]; then
  echo "Unable to read the current version in 'package.json'. Please check version format is: x.y.z (x and y should be an integer)."
  exit 1;
fi
echo "Current version: $current"

### Get current version for Android
currentAndroid=`grep -oP "android-versionCode=\"[0-9]+\"" config.xml | grep -oP "\d+"`
if [[ "_$currentAndroid" == "_" ]]; then
  echo "Unable to read the current Android version in 'config.xml'. Please check version format is an integer."
  exit 1;
fi
echo "Current Android version: $currentAndroid"

# Check version format
if [[ ! $2 =~ ^[0-9]+.[0-9]+.[0-9]+((a|b)[0-9]+)?$ || ! $3 =~ ^[0-9]+$ ]]; then
  echo "Wrong version format"
  echo "Usage:"
  echo " > ./release.sh [pre|rel] <version>  <android-version> <release_description>"
  echo "with:"
  echo " - pre: use for pre-release"
  echo " - rel: for full release"
  echo " - version: x.y.z"
  echo " - android-version: nnn"
  echo " - release_description: a comment on release"
  exit 1
fi

echo "new build version: $2"
echo "new build android version: $3"

case "$1" in
rel|pre)
    # Change the version in files: 'package.json' and 'config.xml'
    sed -i "s/version\": \"$current\"/version\": \"$2\"/g" package.json
    currentConfigXmlVersion=`grep -oP "version=\"\d+.\d+.\d+((a|b)[0-9]+)?\"" config.xml | grep -oP "\d+.\d+.\d+((a|b)[0-9]+)?"`
    sed -i "s/ version=\"$currentConfigXmlVersion\"/ version=\"$2\"/g" config.xml
      sed -i "s/ android-versionCode=\"$currentAndroid\"/ android-versionCode=\"$3\"/g" config.xml

    # Change version in file: 'src/assets/manifest.json'
    currentManifestJsonVersion=`grep -oP "version\": \"\d+.\d+.\d+((a|b)[0-9]+)?\"" src/assets/manifest.json | grep -oP "\d+.\d+.\d+((a|b)[0-9]+)?"`
    sed -i "s/version\": \"$currentManifestJsonVersion\"/version\": \"$2\"/g" src/assets/manifest.json

    # Bump the install.sh
    sed -i "s/echo \"v.*\" #lastest/echo \"v$2\" #lastest/g" install.sh
    ;;
*)
    echo "No task given"
    ;;
esac

echo "----------------------------------"
echo "- Compiling sources..."
echo "----------------------------------"
npm run build.prod
[[ $? -ne 0 ]] && exit 1

echo "----------------------------------"
echo "- Creating web artifact..."
echo "----------------------------------"
mkdir -p "${PROJECT_DIR}/dist"
ZIP_FILE=${PROJECT_DIR}/dist/${PROJECT_NAME}.zip
if [[ -f "$ZIP_FILE" ]]; then
  rm $ZIP_FILE
fi
cd $PROJECT_DIR/www
zip -q -r $ZIP_FILE .
if [[ $? -ne 0 ]]; then
  echo "Connot create the archive for the web artifact"
  exit 1
fi

echo "----------------------------------"
echo "- Compiling sources for Android platform..."
echo "----------------------------------"

# Removing previous APK..."
rm ${PROJECT_DIR}/platforms/android/app/build/outputs/apk/release/*.apk

# Copy generated i18n files, to make sure Android release will use it
cp ${PROJECT_DIR}/www/assets/i18n/*.json ${PROJECT_DIR}/src/assets/i18n/

# Launch the build script
PROJECT_DIR=${PROJECT_DIR}
cd ${PROJECT_DIR}/scripts || exit 1
./release-android.sh
[[ $? -ne 0 ]] && exit 1

echo "----------------------------------"
echo "- Executing git push, with tag: v$2"
echo "----------------------------------"

description="$4"
if [[ "_$description" == "_" ]]; then
    description="Release v$2"
fi

# Commit
cd $PROJECT_DIR
git reset HEAD
git add package.json config.xml src/assets/manifest.json install.sh
git commit -m "v$2"
git tag -f -a "v$2" -m "${description}"
git push origin "v$2"
[[ $? -ne 0 ]] && exit 1

# Pause (if propagation is need between hosted git server and github)
sleep 10s

echo "**********************************"
echo "* Uploading artifacts to Github..."
echo "**********************************"

cd $PROJECT_DIR/scripts
./release-to-github.sh $1 ''"$description"''
[[ $? -ne 0 ]] && exit 1

#echo "----------------------------------"
#echo "- Building desktop artifacts..."
#echo "----------------------------------"

#git submodule init
#git submodule sync
#git submodule update --remote --merge

#if [[ -d "$PROJECT_DIR/platforms/desktop" ]]; then
#  cd platforms/desktop

#  # Build desktop assets
#  ./release.sh $2
#  if [[ $? -ne 0 ]]; then
#      exit 1
#  fi
#else
#  echo "WARN: platform/desktop not found -> Skipping desktop build!"
#fi;

# back to nodejs version 6
#cd $PROJECT_DIR
#nvm use 10

echo "**********************************"
echo "* Build release succeed !"
echo "**********************************"

