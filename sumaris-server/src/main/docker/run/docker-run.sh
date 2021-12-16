#!/bin/bash

CI_REGISTRY=gitlab-registry.ifremer.fr
CI_PROJECT_NAME=sumaris-pod
CI_PROJECT_PATH=sih-public/sumaris/sumaris-pod
CI_REGISTRY_IMAGE_PATH=${CI_REGISTRY}/${CI_PROJECT_PATH}
CI_REGISTER_USER=
CI_REGISTER_PWD=

if [[ $# -lt 2 || (! $1 =~ ^[0-9]+.[0-9]+.[0-9]+(-(alpha|beta|rc|SNAPSHOT)[-0-9]*)?$ && ! $1 = 'develop' ) ]]; then
  echo "ERROR: Invalid arguments"
  echo " Usage: $0 <version> <port>"
  exit 1
fi

SCRIPT_DIR=$(dirname $0)
BASEDIR=$(cd "${SCRIPT_DIR}" && pwd -P)

VERSION=$1
PORT=$2
PROFILES=$3
CONFIG=${BASEDIR}/config
CI_REGISTRY_IMAGE=${CI_REGISTRY_IMAGE_PATH}:${VERSION}
CONTAINER_PREFIX="${CI_PROJECT_NAME}-${PORT}"
CONTAINER_NAME="${CONTAINER_PREFIX}-${VERSION}"

# Starting log
echo "Starting ${CI_PROJECT_NAME} v${APP_VERSION} on port ${PORT} (profiles: '${PROFILES}', config: '${CONFIG})'}"

## Login to container registry
echo "Login to ${CI_REGISTRY}..."
docker login -u "${CI_REGISTER_USER}" -p "${CI_REGISTER_PWD}" ${CI_REGISTRY}
[[ $? -ne 0 ]] && exit 1

# Pull the expected image
echo "Pulling image ${CI_REGISTRY_IMAGE}"
docker pull --no-cache ${CI_REGISTRY_IMAGE}
[[ $? -ne 0 ]] && exit 1

# Logout from container registry
docker logout ${CI_REGISTRY}

# Stop existing container
if [[ ! -z  $(docker ps -f name=${CONTAINER_PREFIX} -q) ]]; then
  echo "Stopping running instance..."
  docker stop $(docker ps -f name=${CONTAINER_PREFIX} -q)
fi

if [[ ! -d "${CONFIG}" ]]; then
  echo "ERROR: Config (directory or file) not found: '${CONFIG}'"
  exit 1
fi

docker run -it -d --rm \
           --name "${CONTAINER_NAME}" \
           -p ${PORT}:${PORT} \
           -v ${CONFIG}:/app/config   \
           -v /home/tnsnames:/home/tnsnames \
           -e PROFILES=${PROFILES} \
           -e PORT=${PORT} \
           -e TZ=UTC \
           ${CI_REGISTRY_IMAGE}
