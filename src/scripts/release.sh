#!/bin/bash

# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  PROJECT_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
  export PROJECT_DIR
fi;

if [[ -f "${PROJECT_DIR}/.local/env.sh" ]]; then
  source ${PROJECT_DIR}/.local/env.sh
fi;

cd ${PROJECT_DIR}

### Control that the script is run on `develop` branch
branch=`git rev-parse --abbrev-ref HEAD`
if [[ ! "$branch" = "develop" ]];
then
  echo ">> This script must be run under \`develop\` branch"
  exit 1
fi

task=$1
version=$2
release_description=$3

# Check arguments
if [[ ! $task =~ ^(pre|rel)$ || ! $version =~ ^[0-9]+.[0-9]+.[0-9]+(-(alpha|beta|rc)[0-9]+)?$ ]]; then
  echo "Wrong version format"
  echo "Usage:"
  echo " > $0 pre|rel <version> <release_description>"
  echo "with:"
  echo " - pre: use for pre-release"
  echo " - rel: for full release"
  echo " - version: x.y.z"
  echo " - release_description: a comment on release"
  exit 1
fi

echo "---- Creating release $version ($task)"...
echo ""

# Removing existing release branche
git branch -D "release/$version" || true

echo "---- Preparing release..."
mvn -B gitflow:release-start -DreleaseVersion="$version"
[[ $? -ne 0 ]] && exit 1
echo "---- Prepare release [OK]"
echo ""

echo "---- Performing release..."
mvn clean deploy -DperformRelease -DskipTests -Dspring.datasource.platform=hsqldb
[[ $? -ne 0 ]] && exit 1
echo "---- Perform release [OK]"
echo ""

echo "---- Generating DB..."
cd ${PROJECT_DIR}/sumaris-core
version=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`

# Generate the DB (run InitTest class)
mvn -Prun,hsqldb -DskipTests --quiet
[[ $? -ne 0 ]] && exit 1
# Create ZIP
cd target
zip -q -r "sumaris-db-$version.zip" db
[[ $? -ne 0 ]] && exit 1

echo "---- Generate DB [OK]"
echo ""

echo "---- Pushing changes to upstream..."
cd ${PROJECT_DIR}
git commit -a -m "Release $version\n$release_description"
git status
mvn gitflow:release-finish
[[ $? -ne 0 ]] && exit 1

echo "---- Push changes to upstream [OK]"
echo ""

echo "---- Removing local release branch ..."
echo ""
git branch -d "release/$version"
# NOTE: can fail, but continu

echo "---- Uploading artifacts to Github..."
echo ""

# Pause (wait propagation to from gitlab to github)
echo " Waiting 40s, for propagation to github..." && sleep 40s

. ${PROJECT_DIR}/src/scripts/release-to-github.sh $task $version ''"$release_description"''
[[ $? -ne 0 ]] && exit 1

echo "---- Uploading artifacts to Github [OK]"
echo ""

echo "----------------------------------"
echo "RELEASE finished!"
echo "----------------------------------"
