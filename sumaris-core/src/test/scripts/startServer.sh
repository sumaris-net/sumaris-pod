#!/bin/sh
export HOME=`eval echo "~$USER"`
export M2_REPO="$HOME/.m2/repository"
export CLASSPATH="$M2_REPO/org/hsqldb/hsqldb/2.4.1/hsqldb-2.4.1.jar"
export DB_NAME="sumaris"
export TEST_DB="../../../target/db"
export DB_DIRECTORY="../../../target/db-server"

# Make sure test DB exists
if [ ! -f "${TEST_DB}/${DB_NAME}.script" ]; then
    echo "Test DB not exists. Please run InitTest first !"
    exit
fi;

# Copy stest DB
echo "Copy test DB into 'target/db-server'"
rm -rf ${DB_DIRECTORY}
cp -R ../../../target/db ${DB_DIRECTORY}
# Change 'readonly' value to false
sed -i 's:^[ \t]*readonly[ \t]*=\([ \t]*.*\)$:readonly=false:' "${DB_DIRECTORY}/${DB_NAME}.properties"

cp $CLASSPATH .

export DB_OPTS="--database.0 file:${DB_DIRECTORY}/${DB_NAME} --dbname.0 ${DB_NAME}"

#export DB_TEMP_DIRECTORY="../db-temp"
#export DB_OPTS=$DB_OPTS --database.1 file:$DB_TEMP_DIRECTORY/${DB_NAME} --dbname.1 ${DB_NAME}-temp

# run db-server and db-server-prod
java -classpath $CLASSPATH org.hsqldb.Server $DB_OPTS
