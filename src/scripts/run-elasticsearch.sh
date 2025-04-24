#!/bin/bash -e

SCRIPT_DIR=$(dirname "$(readlink "$BASH_SOURCE" || echo "$BASH_SOURCE")")
PROJECT_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd -P)
export PROJECT_DIR

cd $PROJECT_DIR

# Variables
export ES_VERSION="7.17.7"

# Override with a local file, if any
if [[ -f "${PROJECT_DIR}/.local/env.sh" ]]; then
  echo "Loading environment variables from: '.local/env.sh'"
  . ${PROJECT_DIR}/.local/env.sh
else
  echo "No file '${PROJECT_DIR}/.local/env.sh' found. Will use defaults"
fi

# For ES 7.17.0 +
export ES_JAVA_HOME=${JAVA_HOME}
export ES_PATH_HOME=${PROJECT_DIR}/.local/elasticsearch-${ES_VERSION}
export ES_PATH_CONF=${ES_PATH_HOME}/config
#export ES_JAVA_OPTS="-Xmx512M -XX:MaxPermSize=512m -Djava.security.policy=file://${ES_PATH_CONF}/security.policy"

if [ ! -d "${ES_PATH_HOME}" ]; then
  es_target="$(dirname ${ES_PATH_HOME})/elasticsearch-${ES_VERSION}-linux-x86_64.tar.gz"
  if [ ! -f "${es_target}" ]; then
    echo "Download elasticsearch ${ES_VERSION}..."
    curl "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ES_VERSION}-linux-x86_64.tar.gz" -o "${es_target}"
  fi
  echo "Install elasticsearch ${ES_VERSION}..."
  tar -x -f "${es_target}" -C "$(dirname ${ES_PATH_HOME})" && rm "${es_target}"
fi

cd "${ES_PATH_HOME}/bin"
./elasticsearch