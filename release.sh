#!/bin/bash

echo "**********************************"
echo "* Preparing release..."
echo "**********************************"
tmp=`mvn release:clean`
result=`mvn release:prepare`

failure=`echo "$result" | grep -m1 -P "\[INFO\] BUILD FAILURE"  | grep -oP "BUILD \w+"`

# prepare failed
if [[ ! "_$failure" = "_" ]]; then
    echo "$result" | grep -P "\[ERROR\] "
    exit
fi

echo "**********************************"
echo "* Performing release..."
echo "**********************************"
result=`mvn release:perform`

failure=`echo "$result" | grep -m1 -P "\[INFO\] BUILD FAILURE"  | grep -oP "BUILD \w+"`

# perform failed
if [[ ! "_$failure" = "_" ]]; then
    echo "$result" | grep -P "\[ERROR\] "
    exit
fi

echo "**********************************"
echo "* Uploading artifacts to Github..."
echo "**********************************"
cd target/checkout
./github.sh pre


