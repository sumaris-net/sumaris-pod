#!/bin/bash


# --- User variables (can be redefined): ---------------------------------------

SERVICE_NAME=sumaris-pod
VERSION=@project.version@
PROFILE=hsqldb
TIMEZONE=UTC

# - Optional vars:
#SUMARIS_HOME=/path/to/sumaris/home
#SUMARIS_LOG="${SUMARIS_HOME}/logs/${SERVICE_NAME}.log"
#JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
#JAVA_OPTS="-Xms2g -Xmx2g -Xverify:none"

# --- Fixed variables (DO NOT changes):  --------------------------------------

WAR_FILENAME="@project.parent.artifactId@-${VERSION}.@project.packaging@"
REPO_URL="https://github.com/sumaris-net/sumaris-pod"
WAR_URL="${REPO_URL}/releases/download/${VERSION}/${WAR_FILENAME}"
JAVA_VERSION=1.8.121
JAVA_JRE_URL=https://nexus.e-is.pro/nexus/service/local/repositories/jvm/content/com/oracle/jre/${JAVA_VERSION}/jre-${JAVA_VERSION}-linux-x64.zip
DEFAULT_JVM_OPTS="-Xms512m -Xmx1024m -Xverify:none"

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
if [[ "_${JAVA_HOME}" == "_" ]]; then
  JAVA_HOME="${SUMARIS_HOME}/lib/jre-${JAVA_VERSION}"
fi
if [[ "${JAVA_OPTS}_" == "_" ]]; then
  JAVA_OPTS="${DEFAULT_JVM_OPTS}"
fi

PID_FILE="${DATA_DIRECTORY}/${SERVICE_NAME}.pid"
WAR_FILE="${SUMARIS_HOME}/lib/${WAR_FILENAME}"
JAVA_EXEC=${JAVA_HOME}/bin/java

JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=${SUMARIS_HOME}/config/"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.basedir=${SUMARIS_HOME}"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.data.directory=${DATA_DIRECTORY}"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.log.file=${SUMARIS_LOG}"
[[ "_${PROFILE}" != "_" ]]  && JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${PROFILE}"
[[ "_${TIMEZONE}" != "_" ]] && JAVA_OPTS="$JAVA_OPTS -Duser.timezone=${TIMEZONE}"

APP_ARGS=${@:2}
JAVA_CMD="${JAVA_EXEC} -server $JAVA_OPTS -jar $WAR_FILE $APP_ARGS"

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

# Make sure Java JRE version is correct
checkJreVersion() {

    JAVA_VERSION_TEST=$(${JAVA_EXEC} -version 2>&1 | egrep "(java|openjdk) version" | awk '{print $3}' | tr -d \")
    if [[ $? -ne 0 ]]; then
        echo "Invalid Java JRE. Command '${JAVA_EXEC} -version' failed!"
        exit 1
    fi
    JAVA_MAJOR_VERSION=`echo ${JAVA_VERSION_TEST} | awk '{split($0, array, ".")} END{print array[1]}'`
    JAVA_MINOR_VERSION=`echo ${JAVA_VERSION_TEST} | awk '{split($0, array, ".")} END{print array[2]}'`
    if [[ ${JAVA_MAJOR_VERSION} -ne 1 ]] || [[ ${JAVA_MINOR_VERSION} -ne 8 ]]; then
        echo "Require a Java JRE in version 1.8, but found ${JAVA_VERSION_TEST}. Please check 'JAVA_HOME' is valid, at '$0'."
        exit 1
    fi
    echo "Java JRE: ${JAVA_VERSION_TEST}"
}

# Make sure Java JRE exists
checkJreExists() {
  if [ -f "${JAVA_EXEC}" ]; then
    checkJreVersion
  else

    # Try to use the default 'java'
    JAVA_VERSION_TEST=$(java -version 2>&1 | egrep "(java|openjdk) version" | awk '{print $3}' | tr -d \")
    if [[ $? -eq 0 ]]; then
      JAVA_EXEC="java"
      checkJreVersion

    # Install a fresh Java JRE
    else
        installJre
    fi
  fi;

}

# Install a fresh Java JRE
installJre() {
  if [[ ! -f "${JAVA_EXEC}" ]]; then
    TMP_FILE="/tmp/jre-${JAVA_VERSION}.zip"
    TMP_DIR="/tmp/jre-${JAVA_VERSION}"
    echo "Downloading Java JRE ${JAVA_VERSION}...  ${JAVA_JRE_URL}"
    download "${JAVA_JRE_URL}" -o "${TMP_FILE}"
    [[ $? -eq 0 ]] && unzip "${TMP_FILE}" -d "${TMP_DIR}"
    if [[ $? -ne 0 ]]; then
        echo "ERROR - Missing Java JRE file at: ${JAVA_HOME}"
        echo " Please download it manually: ${JAVA_JRE_URL}"
        echo " or check JAVA_HOME is valid, at: ${JAVA_HOME}"
        exit 1
    fi
    # If root is 'jre'
    if [[ -d "${TMP_DIR}/jre" ]]; then
        TMP_DIR="${TMP_DIR}/jre"
    fi

    mkdir -p "${JAVA_HOME}"
    mv ${TMP_DIR}/* "${JAVA_HOME}/" && rm "${TMP_FILE}" && rm -rf "${TMP_DIR}"
    [[ $? -ne 0 ]] && exit 1
  fi;
}

# Make sure Pod JAR exists
checkJarExists() {
  if [ ! -f "${WAR_FILE}" ]; then
    echo "Downloading Pod WAR file: ${WAR_URL}..."
    download "$WAR_URL" -o "${WAR_FILE}"
    if [[ $? -ne 0 ]]; then
      echo "ERROR - Missing Pod WAR file: ${WAR_FILE}"
      echo " Please download it manually: ${WAR_URL}"
      echo " and save it into the directory: ${SUMARIS_HOME}/lib/"
      exit 1
    fi
  fi;
  echo "Pod version: ${VERSION}"
}

start() {
  checkJreExists
  checkJarExists

  echo "Starting $SERVICE_NAME..."
  echo " - JAVA_OPTS: $JAVA_OPTS"
  echo " - log: $SUMARIS_LOG"

  mkdir -p ${SUMARIS_LOG_DIR}

  cd $SUMARIS_HOME

  echo $JAVA_CMD
  $JAVA_CMD

  #PID=`nohup $JAVA_CMD >> /dev/null 2>> /dev/null & echo $!`
}

case "$1" in
install)
    checkJarExists
    installJre
    checkJreExists
    echo "Successfully installed!"
    ;;

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

-version)
    checkJarExists
    checkJreExists
    ;;

*)
    echo "Usage: $0 {status|start|stop|restart}"
    exit 0
esac
