#!/bin/bash

mkdir -p .local

RELEASE_OPTS="-DskipTests"

# Rollback previous release, if need
if [[ -f "pom.xml.releaseBackup" ]]; then
    echo "**********************************"
    echo "* Rollback previous release..."
    echo "**********************************"
    result=`mvn release:rollback`
    failure=`echo "$result" | grep -m1 -P "\[INFO\] BUILD FAILURE"  | grep -oP "BUILD \w+"`
    # rollback failed
    if [[ ! "_$failure" = "_" ]]; then
        echo "$result" | grep -P "\[ERROR\] "
        exit 1
    fi
    echo "Rollback previous release [OK]"
fi


echo "**********************************"
echo "* Preparing release..."
echo "**********************************"
mvn release:prepare -Darguments="${RELEASE_OPTS}"
if [[ $? -ne 0 ]]; then
    exit 1
fi
echo "Prepare release [OK]"


echo "**********************************"
echo "* Performing release..."
echo "**********************************"
mvn release:perform -Darguments="-DskipTests -Denv=hsqldb"
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
echo "Generate DB [OK]"


echo "**********************************"
echo "* Pushing changes to upstream..."
echo "**********************************"
# Push git
cd $dirname
git push
if [[ $? -ne 0 ]]; then
    exit
fi

# Pause (if propagation is need between hosted git server and github)
sleep 10s
echo "Push changes to upstream [OK]"


echo "**********************************"
echo "* Uploading artifacts to Github..."
echo "**********************************"
cd $dirname/target/checkout
./github.sh pre
if [[ $? -ne 0 ]]; then
    exit 1
fi
echo "Upload artifacts to github [OK]"


echo "----------------------------------"
echo "RELEASE finished !"
echo "----------------------------------"

echo "Rebuild new SNAPSHOT version..."
mvn clean install -DSkipTests --quiet
if [[ $? -ne 0 ]]; then
    exit 1
fi
echo "Rebuild new SNAPSHOT version [OK]"


