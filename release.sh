#!/bin/bash

mkdir -p .local

echo "**********************************"
echo "* Preparing release..."
echo "**********************************"
#result=`mvn release:clean`
#failure=`echo "$result" | grep -m1 -P "\[INFO\] BUILD FAILURE"  | grep -oP "BUILD \w+"`
# prepare failed
if [[ ! "_$failure" = "_" ]]; then
    echo "$result" | grep -P "\[ERROR\] "
    exit 1
fi

mvn release:prepare
if [[ $? -ne 0 ]]; then
    exit 1
fi

echo "**********************************"
echo "* Performing release..."
echo "**********************************"
mvn release:perform -Darguments="-DskipTests" --quiet
if [[ $? -ne 0 ]]; then
    exit 1
fi

echo "**********************************"
echo "* Generating DB..."
echo "**********************************"
dirname=`pwd`
cd $dirname/target/checkout/sumaris-core
version=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`

# Generate the DB (run InitTest class)
mvn -Prun,hsqldb -DskipTests --quiet
if [[ $? -ne 0 ]]; then
    exit 1
fi

# Create ZIP
cd target
zip -q -r "sumaris-db-$version.zip" db
if [[ $? -ne 0 ]]; then
    exit 1
fi

# Push git
cd $dirname
git push
if [ $? -ne 0 ]; then
    exit
fi

# Pause (if propagation is need between hosted git server and github)
sleep 10s

echo "**********************************"
echo "* Uploading artifacts to Github..."
echo "**********************************"
cd $dirname/target/checkout
./github.sh pre
if [[ $? -ne 0 ]]; then
    exit 1
fi

echo "RELEASE finished !"

