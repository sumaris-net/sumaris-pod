#!/bin/bash

# Get to the root project
if [[ "_" == "_${CI_PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  CI_PROJECT_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
  export CI_PROJECT_DIR
fi;
cd ${CI_PROJECT_DIR}

task=$1
dir=$2
if [[ "_" == "_${dir}" ]]; then
  dir=/tmp/.build-cache
fi;

DEPS_FILENAME=dependencies.json
DEV_DEPS_FILENAME=devDependencies.json

PROJECT_NAME=$(node -e "console.log(require('./package.json').name)")
PROJECT_DEPENDENCIES=$(node -e "console.log(require('./package.json').dependencies)")
PROJECT_DEV_DEPENDENCIES=$(node -e "console.log(require('./package.json').devDependencies)")

BUILD_CACHE_DIR=${dir}/${PROJECT_NAME}/dependencies
PROJECT_CACHE_DIR=${CI_PROJECT_DIR}/.cache/dependencies

case "$task" in
  store)
    mkdir -p ${BUILD_CACHE_DIR}
    echo "${PROJECT_DEPENDENCIES}" > ${BUILD_CACHE_DIR}/${DEPS_FILENAME}
    echo "${PROJECT_DEV_DEPENDENCIES}" > ${BUILD_CACHE_DIR}/${DEV_DEPS_FILENAME}
    echo "Project dependencies successfully stored at: ${BUILD_CACHE_DIR}"
  ;;

  check)
    echo "---- Checking project dependencies (from package.json)..."
    echo " Project cache dir: ${PROJECT_CACHE_DIR}"
    echo "   Build cache dir: ${BUILD_CACHE_DIR}"

    mkdir -p ${PROJECT_CACHE_DIR}

    echo "--- Checking 'dependencies' changes:"
    if [[ ! -f ${BUILD_CACHE_DIR}/${DEPS_FILENAME} ]]; then
      echo "ERROR: Cannot check dependencies: missing file ${BUILD_CACHE_DIR}/${DEPS_FILENAME} - Please execute '$0 store' then retry."
      exit 1
    fi
    echo "${PROJECT_DEPENDENCIES}" > ${PROJECT_CACHE_DIR}/${DEPS_FILENAME}
    diff ${BUILD_CACHE_DIR}/${DEPS_FILENAME} ${PROJECT_CACHE_DIR}/${DEPS_FILENAME}
    [[ $? -ne 0 ]] && exit 1
    echo " (no changes)"

    echo "--- Checking 'devDependencies' changes:"
    if [[ ! -f ${BUILD_CACHE_DIR}/${DEV_DEPS_FILENAME} ]]; then
      echo "ERROR: missing file ${BUILD_CACHE_DIR}/${DEV_DEPS_FILENAME} - Please execute '$0 store' then retry."
      exit 1
    fi
    echo "${PROJECT_DEV_DEPENDENCIES}" > ${PROJECT_CACHE_DIR}/${DEV_DEPS_FILENAME}
    diff ${BUILD_CACHE_DIR}/${DEV_DEPS_FILENAME} ${PROJECT_CACHE_DIR}/${DEV_DEPS_FILENAME}
    [[ $? -ne 0 ]] && exit 1
    echo " (no changes)"
  ;;

  *)
    echo "Wrong arguments"
    echo "Usage:"
    echo " > $0 store|check"
    echo "With:"
    echo " - store: Save project dependencies, to be able to check it later"
    echo " - check: Check there is no changes in dependencies"
    exit 1
    ;;
esac
