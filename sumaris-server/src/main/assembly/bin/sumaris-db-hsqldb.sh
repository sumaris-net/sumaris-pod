#!/bin/bash


# --- User variables (can be redefined): ---------------------------------------

SERVICE_NAME=sumaris-db
DB_NAME=sumaris
DB_PORT=9001
TIMEZONE=UTC

# - Optional vars:
#SUMARIS_HOME=/path/to/sumaris/home
#SUMARIS_LOG=${SUMARIS_HOME}/logs/${SERVICE_NAME}.log
#JAVA_OPTS="-Xms2g -Xmx2g"

# - For backup the DB in a remote server, using FTP
#BACKUP_FTP_URL=ftp://user:pwd@host.domain.com
#BACKUP_REMOTE_DIR=/remote/backup/dir

# --- Fixed variables (DO NOT changes):  --------------------------------------

HSQLDB_VERSION=@hsqldb.version@
TEST_DB_VERSION=@project.version@
#TEST_DB_VERSION=1.12.5

HSQLDB_M2_REPO="https://nexus.e-is.pro/nexus/service/local/repositories/central/content"
#HSQLDB_M2_REPO="http://www.hsqldb.org/repos"

TEST_DB_GITHUB_REPO="https://github.com/sumaris-net/sumaris-pod"

# --- Program start -----------------------------------------------------------

if [ "${SUMARIS_HOME}_" == "_" ]; then
  SCRIPT_DIR=$(dirname $0)
  SUMARIS_HOME=$(cd ${SCRIPT_DIR}/.. && pwd)
  #echo "Using Sumaris home: ${SUMARIS_HOME}"
fi
if [ "${SUMARIS_LOG_DIR}_" == "_" ]; then
  SUMARIS_LOG_DIR="${SUMARIS_HOME}/logs"
fi
if [ "${SUMARIS_LOG}_" == "_" ]; then
  SUMARIS_LOG="${SUMARIS_LOG_DIR}/${SERVICE_NAME}.log"
fi
if [ "${DATA_DIRECTORY}_" == "_" ]; then
  DATA_DIRECTORY="${SUMARIS_HOME}/data"
fi
if [[ "${JAVA_OPTS}_" == "_" ]]; then
  JAVA_OPTS="-Xms512m -Xmx1024m"
fi

JAR_URL="${HSQLDB_M2_REPO}/org/hsqldb/hsqldb/${HSQLDB_VERSION}/hsqldb-${HSQLDB_VERSION}.jar"
TOOL_JAR_URL="${HSQLDB_M2_REPO}/org/hsqldb/sqltool/${HSQLDB_VERSION}/sqltool-${HSQLDB_VERSION}.jar"
TEST_DB_URL="${TEST_DB_GITHUB_REPO}/releases/download/${TEST_DB_VERSION}/sumaris-db-${TEST_DB_VERSION}.zip"

PID_FILE="${DATA_DIRECTORY}/${SERVICE_NAME}.pid"
JAR_FILE="${SUMARIS_HOME}/lib/hsqldb-${HSQLDB_VERSION}.jar"
TOOL_JAR_FILE="${SUMARIS_HOME}/lib/sqltool-${HSQLDB_VERSION}.jar"
DB_DIRECTORY=${DATA_DIRECTORY}/db
DB_PATH=${DB_DIRECTORY}/sumaris
SUMARIS_LOG="${SUMARIS_LOG_DIR}/sumaris-db.log"

# WARN: with trailing slash
BACKUP_DIR="${DATA_DIRECTORY}/db-backup/"
BACKUP_LOG="${SUMARIS_LOG_DIR}/sumaris-db-backup.log"

mkdir -p ${SUMARIS_HOME}/lib
mkdir -p ${SUMARIS_HOME}/bin
mkdir -p ${SUMARIS_LOG_DIR}
mkdir -p ${DATA_DIRECTORY}
mkdir -p ${DB_DIRECTORY}

[[ "_${TIMEZONE}" != "_" ]] && JAVA_OPTS="$JAVA_OPTS -Duser.timezone=${TIMEZONE}"

SERVER_ARGS="--database.0 file:${DB_PATH} --dbname.0 ${DB_NAME} --port ${DB_PORT}"
JAVA_CMD="java -server $JAVA_OPTS -classpath $JAR_FILE org.hsqldb.Server $SERVER_ARGS"

# Check if a software is installed
is_installed() {
  type "$1" > /dev/null 2>&1
}

# Download using wget or curl
download() {
  if is_installed "curl"; then
    curl -qkL $*
  elif is_installed "wget"; then
    # Emulate curl with wget
    ARGS=$(echo "$*" | command sed -e 's/--progress-bar /--progress=bar /' \
                           -e 's/-L //' \
                           -e 's/-I /--server-response /' \
                           -e 's/-s /-q /' \
                           -e 's/-o /-O /' \
                           -e 's/-C - /-c /')
    wget $ARGS
  fi
}

# Make sure jar exists
checkJarExists() {
  if [[ ! -f "${TOOL_JAR_FILE}" ]]; then
    echo "--- Downloading Hsqldb Tools JAR...  ${TOOL_JAR_URL}"
    download "${TOOL_JAR_URL}" -o "${TOOL_JAR_FILE}"
    if [[ $? -ne 0 ]]; then
      echo "ERROR - Missing Hsqldb JAR file at: ${TOOL_JAR_FILE}"
      echo " Please download it manually: ${TOOL_JAR_URL}"
      echo " and save it into the directory: ${SUMARIS_HOME}/lib/"
      exit 1
    fi
  fi;
  if [ ! -f "${JAR_FILE}" ]; then
    echo "--- Downloading Hsqldb JAR...  ${JAR_URL}"
    download "${JAR_URL}" -o "${JAR_FILE}"
    if [[ $? -ne 0 ]]; then
      echo "ERROR - Missing Hsqldb JAR file at: ${JAR_FILE}"
      echo " Please download it manually: ${JAR_URL}"
      echo " and save it into the directory: ${SUMARIS_HOME}/lib/"
      exit 1
    fi
  fi;
}

# Make sure DB exists
checkDbExists() {

  if [ ! -f "${DB_PATH}.script" ]; then
    TMP_FILE="/tmp/sumaris-db-${TEST_DB_VERSION}.zip"
    echo "--- Downloading test DB v${TEST_DB_VERSION}..."
    download "$TEST_DB_URL" -o "$TMP_FILE"
    [[ $? -eq 0 ]] && unzip $TMP_FILE -d $DATA_DIRECTORY
    if [[ $? -ne 0 ]]; then
      echo "ERROR - Missing DB files at: $DB_DIRECTORY"
      echo " Please download it manually: ${JAR_URL}"
      echo " and copy it into the directory: ${SUMARIS_HOME}/lib/"
      exit 1
    fi
    rm ${TMP_FILE}

    # Change readonly property to 'false'
    DB_PROPERTY_FILE="${DB_PATH}.properties"
    echo "version=${HSQLDB_VERSION}" > $DB_PROPERTY_FILE
    echo "readonly=false" >> $DB_PROPERTY_FILE
    echo "tx_timestamp=0" >> $DB_PROPERTY_FILE
    echo "modified=no" >> $DB_PROPERTY_FILE

    echo "SUMARIS test DB successfully installed into: ${DB_DIRECTORY}"
  fi;
}

checkToolRcFile() {
  # Create sqltool.rc file
  # => Make sure to always re-create this file, because variables can be changed !
  TOOL_RC_FILE="${SUMARIS_HOME}/bin/sqltool.rc"
  rm -f $TOOL_RC_FILE

  if [ ! -f "${TOOL_RC_FILE}" ]; then
    echo "--- Creating HSQLDB tool RC file: $TOOL_RC_FILE ..."
    echo "urlid ${DB_NAME}" > $TOOL_RC_FILE
    echo "url jdbc:hsqldb:hsql://127.0.0.1:${DB_PORT}/${DB_NAME}" >> $TOOL_RC_FILE
    echo "username sa" >> $TOOL_RC_FILE
    echo "password" >> $TOOL_RC_FILE
  fi
}

start() {
  checkJarExists
  checkDbExists

  echo "--- Starting $SERVICE_NAME..."
  echo " - args: $SERVER_ARGS"
  echo " - log:  $SUMARIS_LOG"

  echo "$JAVA_CMD"
  #$JAVA_CMD
  PID=`nohup $JAVA_CMD >> $SUMARIS_LOG 2>> $SUMARIS_LOG & echo $!`
}

backup() {
  checkJarExists
  checkToolRcFile

  FTP_URL=${BACKUP_FTP_URL}
  FTP_REMOTE_DIR=${BACKUP_REMOTE_DIR}

  echo "--- Starting backup of $SERVICE_NAME..."
  echo "Starting backup of $SERVICE_NAME..." >> $BACKUP_LOG
  echo " - dir: $BACKUP_DIR"
  echo " - log: $BACKUP_LOG"
  if [ "_${FTP_URL}" != "_" ]; then
    echo " - FTP dir: $FTP_REMOTE_DIR"
  else
    echo " - FTP: /!\ skipped (not configured)! Please set environment variable 'BACKUP_FTP_URL' (e.g. 'ftp://user:pwd@host.domain.com')"
  fi

  # Backup to file
  java -classpath ${JAR_FILE}:${TOOL_JAR_FILE} org.hsqldb.cmdline.SqlTool --rcFile=${TOOL_RC_FILE} --sql="BACKUP DATABASE TO '${BACKUP_DIR}' NOT BLOCKING;" ${DB_NAME} >> ${BACKUP_LOG} 2>> ${BACKUP_LOG}
  if [[ $? -ne 0 ]]; then
    echo "ERROR while executing backup. See log: $BACKUP_LOG"
    exit 1
  fi
  BACKUP_FILE=$(ls -1t $BACKUP_DIR | head -1)
  echo "Created file saved at: ${BACKUP_DIR}${BACKUP_FILE}"

  # FTP upload
  if [[ "_${FTP_URL}" != "_" ]]; then
    echo "--- Uploading file $BACKUP_FILE to FTP..."
    echo "Uploading file $BACKUP_FILE to FTP..." >> $BACKUP_LOG
    lftp $FTP_URL -e "mirror -e -R $BACKUP_DIR $FTP_REMOTE_DIR ; quit" >> $BACKUP_LOG 2>> $BACKUP_LOG
    if [[ $? -ne 0 ]]; then
      echo "ERROR while uploading backup file to FTP. See log: $BACKUP_LOG"
      exit 1
    fi
  fi

  echo "DB backup succeed"
  echo "DB backup succeed" >> $BACKUP_LOG
  exit 0
}

case "$1" in
start)
    if [ -f "${PID_FILE}" ]; then
        PID=`cat "${PID_FILE}"`
        if [ -z "`ps axf | grep ${PID} | grep -v grep`" ]; then
            start
        else
            echo "Already running [$PID]"
            exit 0
        fi
    else
        start
    fi

    if [ -z $PID ]; then
        echo "Failed starting"
        exit 1
    else
        echo $PID > "${PID_FILE}"
        echo "Started [$PID]"
        exit 0
    fi
    ;;

status)
    if [[ -f "${PID_FILE}" ]]; then
        PID=`cat ${PID_FILE}`
        if [ -z "`ps axf | grep ${PID} | grep -v grep`" ]; then
            echo "Not running (process dead but PID file exists)"
            exit 1
        else
            echo "Running [$PID]"
            exit 0
        fi
    else
        echo "Not running"
        exit 0
    fi
    ;;

stop)
    if [ -f "${PID_FILE}" ]; then
        PID=`cat "${PID_FILE}"`
        if [ -z "`ps axf | grep ${PID} | grep -v grep`" ]; then
            echo "Not running (process dead but PID file exists)"
            rm -f "${PID_FILE}"
            exit 1
        else
            PID=`cat "${PID_FILE}"`
            kill -term $PID
            echo "Stopped [$PID]"
            rm -f "${PID_FILE}"
            exit 0
        fi
    else
        echo "Not running (PID not found)"
        exit 0
    fi
    ;;

restart)
    $0 stop
    sleep 10s
    $0 start
;;
backup)
    if [ -f "${PID_FILE}" ]; then
        PID=`cat "${PID_FILE}"`
        if [ -z "`ps axf | grep ${PID} | grep -v grep`" ]; then
            echo "DB not running (process dead but PID file exists)"
            echo "DB not running (process dead but PID file exists)" >> "${BACKUP_LOG}"
            rm -f "${PID_FILE}"
            exit 1
        else
            backup
        fi
    else
        echo "DB not running (PID not found)"
        echo "DB not running (PID not found)" >> "${BACKUP_LOG}"
        exit 1
    fi
;;
*)
    echo "Usage: $0 {start|stop|restart|status|backup}"
    exit 0
esac
