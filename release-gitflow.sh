#!/bin/bash

mkdir -p .local

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
  echo " > ./release-gitflow.sh pre|rel <version> <release_description>"
  echo "with:"
  echo " - pre: use for pre-release"
  echo " - rel: for full release"
  echo " - version: x.y.z"
  echo " - release_description: a comment on release"
  exit 1
fi

echo "task: $task"
echo "new build version: $version"
echo "release description: $release_description"

echo "**********************************"
echo "* Preparing release..."
echo "**********************************"
mvn -B gitflow:release-start -DreleaseVersion="$version"
[[ $? -ne 0 ]] && exit 1
echo "Prepare release [OK]"


echo "**********************************"
echo "* Performing release..."
echo "**********************************"
mvn clean deploy -DperformRelease -DskipTests -Denv=hsqldb
[[ $? -ne 0 ]] && exit 1

echo "**********************************"
echo "* Generating DB..."
echo "**********************************"
dirname=`pwd`
cd $dirname/sumaris-core
version=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`

# Generate the DB (run InitTest class)
mvn -Prun,hsqldb -DskipTests --quiet
[[ $? -ne 0 ]] && exit 1
# Create ZIP
cd target
zip -q -r "sumaris-db-$version.zip" db
[[ $? -ne 0 ]] && exit 1
echo "Generate DB [OK]"


cd $dirname
echo "**********************************"
echo "* Uploading artifacts to Github..."
echo "**********************************"
./github-gitflow.sh "$task" "$version" ''"$release_description"''
[[ $? -ne 0 ]] && exit 1
echo "Upload artifacts to github [OK]"


echo "**********************************"
echo "* Pushing changes to upstream..."
echo "**********************************"
git commit -a -m "Release $version\n$release_description"
git status
mvn gitflow:release-finish -DfetchRemote=false
[[ $? -ne 0 ]] && exit 1

# Pause (if propagation is need between hosted git server and github)
#sleep 10s
#echo "Push changes to upstream [OK]"

echo "----------------------------------"
echo "RELEASE finished !"
echo "----------------------------------"

echo "Rebuild new SNAPSHOT version..."
mvn clean install -DskipTests --quiet
[[ $? -ne 0 ]] && exit 1
echo "Rebuild new SNAPSHOT version [OK]"


