#!/bin/bash

echo "**********************************"
echo "* Preparing release..."
echo "**********************************"
result=`mvn release:clean`
failure=`echo "$result" | grep -m1 -P "\[INFO\] BUILD FAILURE"  | grep -oP "BUILD \w+"`
# prepare failed
if [[ ! "_$failure" = "_" ]]; then
    echo "$result" | grep -P "\[ERROR\] "
    exit
fi

mvn release:prepare --quiet
if [ $? -ne 0 ]; then
    exit
fi

echo "**********************************"
echo "* Performing release..."
echo "**********************************"
mvn release:perform --quiet
if [ $? -ne 0 ]; then
    exit
fi

echo "**********************************"
echo "* Generating DB..."
echo "**********************************"
dirname=`pwd`
cd $dirname/target/checkout/sumaris-core
version=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`

# Genrate the DB (run InitTest class)
mvn -Prun,hsqldb -DskipTests
if [ $? -ne 0 ]; then
    exit
fi

# Create ZIP
cd target
zip -q -r "sumaris-db-$version.zip" db
if [ $? -ne 0 ]; then
    exit
fi

echo "**********************************"
echo "* Uploading artifacts to Github..."
echo "**********************************"
cd $dirname/target/checkout
./github.sh pre
if [ $? -ne 0 ]; then
    exit
fi

echo "RELEASE finished !"

