#!/bin/bash

SCRIPT_DIR=$(dirname $0)
BASEDIR=$(cd "${SCRIPT_DIR}" && pwd -P)

DEFAULT_VERSION=develop
DEFAULT_PORT=8181
DEFAULT_PROFILES=val

VERSION=$1
PORT=$2
PROFILES=$3
USERID=$(id -u)
GROUPID=$(id -g)

[[ "_${VERSION}" = "_" ]] && VERSION=$DEFAULT_VERSION
[[ "_${PORT}" = "_" && "${VERSION}" = "${DEFAULT_VERSION}" ]] && PORT=$DEFAULT_PORT
[[ "_${PROFILES}" = "_" && "${VERSION}" = "${DEFAULT_VERSION}" && "${PORT}" = "${DEFAULT_PORT}" ]] && PROFILES=$DEFAULT_PROFILES

CONFIG_DIR=${BASEDIR}/config
DATA_DIR=${BASEDIR}/data/${PROFILES}
TIMEZONE=Europe/Paris
JVM_MEMORY=1g

CI_REGISTRY=gitlab-registry.ifremer.fr
CI_PROJECT_NAME=sumaris-pod
CI_PROJECT_PATH=sih-public/sumaris/sumaris-pod
CI_REGISTRY_IMAGE_PATH=${CI_REGISTRY}/${CI_PROJECT_PATH}
CI_REGISTER_USER=gitlab+deploy-token
CI_REGISTER_PWD=# TODO <REPLACE_WITH_DEPLOY_TOKEN>
CI_REGISTRY_IMAGE=${CI_REGISTRY_IMAGE_PATH}:${VERSION}

CONTAINER_PREFIX="${CI_PROJECT_NAME}-${PORT}"
CONTAINER_NAME="${CONTAINER_PREFIX}-${VERSION}"
CONTAINER_MEMORY=2g
CONTAINER_GROUPID=9999

# Check arguments
if [[ (! $VERSION =~ ^[0-9]+.[0-9]+.[0-9]+(-(alpha|beta|rc|SNAPSHOT)[-0-9]*)?$ && $VERSION != 'imagine' && $VERSION != 'develop' ) ]]; then
  echo "ERROR: Invalid version"
  echo " Usage: $0 <version> <port> <profile>"
  exit 1
fi
if [[ (! $PORT =~ ^[0-9]+$ ) ]]; then
  echo "ERROR: Invalid port"
  echo " Usage: $0 <version> <port> <profile>"
  exit 1
fi

mkdir -p ${DATA_DIR}

# Log start
echo "--- Starting ${CI_PROJECT_NAME} v${APP_VERSION} on port ${PORT} (profiles: '${PROFILES}', config: '${CONFIG_DIR}, data: '${DATA_DIR}')'}"

## Login to container registry
echo "Login to ${CI_REGISTRY}..."
docker login -u "${CI_REGISTER_USER}" -p "${CI_REGISTER_PWD}" ${CI_REGISTRY}
[[ $? -ne 0 ]] && exit 1

# Pull the expected image
echo "Pulling image ${CI_REGISTRY_IMAGE}"
docker pull ${CI_REGISTRY_IMAGE}
[[ $? -ne 0 ]] && exit 1

# Logout from container registry
docker logout ${CI_REGISTRY}

# Stop existing container
if [[ ! -z  $(docker ps -f name=${CONTAINER_PREFIX} -q) ]]; then
  echo "Stopping running instance..."
  docker stop $(docker ps -f name=${CONTAINER_PREFIX} -q)
fi

if [[ ! -d "${CONFIG_DIR}" ]]; then
  echo "ERROR: Config directory not found: '${CONFIG_DIR}'"
  exit 1
fi

# Waiting container really removed
sleep 3

docker run -it -d --rm \
           --name "${CONTAINER_NAME}" \
           --memory ${CONTAINER_MEMORY} \
           --user ${USERID}:${GROUPID} \
           --group-add ${CONTAINER_GROUPID} \
           -p ${PORT}:${PORT} \
           -v ${CONFIG_DIR}:/app/config   \
           -v /home/tnsnames:/home/tnsnames \
           -v ${DATA_DIR}:/app/data \
           -e PROFILES=${PROFILES} \
           -e PORT=${PORT} \
           -e TZ=${TIMEZONE} \
           -e XMS=${JVM_MEMORY} \
           -e XMX=${JVM_MEMORY} \
           ${CI_REGISTRY_IMAGE}

echo "---- ${CI_PROJECT_NAME} is running !"
echo ""
echo " Available commands:"
echo "    logs: docker logs -f ${CONTAINER_NAME}"
echo "    bash: docker exec -it ${CONTAINER_NAME} bash"
echo "    stop: docker stop ${CONTAINER_NAME}"
echo "  status: docker ps -f name=${CONTAINER_NAME}"