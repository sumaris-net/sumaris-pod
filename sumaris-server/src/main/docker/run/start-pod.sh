#!/bin/bash

SCRIPT_DIR=$(dirname $0)
BASEDIR=$(cd "${SCRIPT_DIR}" && pwd -P)

VERSION=$1
PORT=$2
PROFILES=$3
CONFIG=/home/isi-projets/sih/gestion/tools/imagine/config/
TIMEZONE=UTC
[[ "_${VERSION}" = "_" ]] && VERSION=imagine
[[ "_${PORT}" = "_" && "${VERSION}" = "imagine" ]] && PORT=8080
[[ "_${PROFILES}" = "_" && "${VERSION}" = "imagine" ]] && PROFILES=dev

CI_REGISTRY=gitlab-registry.ifremer.fr
CI_PROJECT_NAME=sumaris-pod
CI_PROJECT_PATH=sih-public/sumaris/sumaris-pod
CI_REGISTRY_IMAGE_PATH=${CI_REGISTRY}/${CI_PROJECT_PATH}
CI_REGISTER_USER=
CI_REGISTER_PWD=
CI_REGISTRY_IMAGE=${CI_REGISTRY_IMAGE_PATH}:${VERSION}
CONTAINER_PREFIX="${CI_PROJECT_NAME}-${PORT}"
CONTAINER_NAME="${CONTAINER_PREFIX}-${VERSION}"

# Check arguments
#if [[ (! $VERSION =~ ^[0-9]+.[0-9]+.[0-9]+(-(alpha|beta|rc|SNAPSHOT)[-0-9]*)?$ && $VERSION != 'imagine' ) ]]; then
#  echo "ERROR: Invalid version"
#  echo " Usage: $0 <version> <port>"
#  exit 1
#fi
if [[ (! $PORT =~ ^[0-9]+$ ) ]]; then
  echo "ERROR: Invalid port"
  echo " Usage: $0 <version> <port>"
  exit 1
fi

# Log start
echo "--- Starting ${CI_PROJECT_NAME} v${APP_VERSION} on port ${PORT} (profiles: '${PROFILES}', config: '${CONFIG})'}"

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

if [[ ! -d "${CONFIG}" ]]; then
  echo "ERROR: Config (directory or file) not found: '${CONFIG}'"
  exit 1
fi

# Waiting container really removed
sleep 3

docker run -it -d --rm \
           --name "${CONTAINER_NAME}" \
           -p ${PORT}:${PORT} \
           -v ${CONFIG}:/app/config   \
           -v /home/tnsnames:/home/tnsnames \
           -e PROFILES=${PROFILES} \
           -e PORT=${PORT} \
           -e TZ=${TIMEZONE} \
           ${CI_REGISTRY_IMAGE}

echo "---- ${CI_PROJECT_NAME} is running !"
echo ""
echo " Available commands:"
echo "    logs: docker logs -f ${CONTAINER_NAME}"
echo "    bash: docker exec -it ${CONTAINER_NAME} bash"
echo "    stop: docker stop ${CONTAINER_NAME}"
echo "  status: docker ps -a ${CONTAINER_NAME}"
