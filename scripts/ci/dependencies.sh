#!/bin/bash

# Get to the root project
if [[ "_" == "_${CI_PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  CI_PROJECT_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
  export CI_PROJECT_DIR
fi;
cd ${CI_PROJECT_DIR}

task=$1

CACHE_DIR=/tmp/.cache/dependencies
DEPS_FILENAME=dependencies.json
DEV_DEPS_FILENAME=devDependencies.json

PROJECT_DEPENDENCIES=$(node -e "console.log(require('./package.json').dependencies)")
PROJECT_DEV_DEPENDENCIES=$(node -e "console.log(require('./package.json').devDependencies)")

case "$task" in
  store)
    mkdir -p ${CACHE_DIR}
    echo "${PROJECT_DEPENDENCIES}" > ${CACHE_DIR}/${DEPS_FILENAME}
    echo "${PROJECT_DEV_DEPENDENCIES}" > ${CACHE_DIR}/${DEV_DEPS_FILENAME}
    echo "Project dependencies stored in file: ${CACHE_DIR}"
  ;;

  check)
    echo "Checking project dependencies..."
    echo " Project dir: ${CI_PROJECT_DIR}"
    echo "   Cache dir: ${CACHE_DIR}"

    PROJECT_CACHE_DIR=${CI_PROJECT_DIR}/.cache
    mkdir -p ${PROJECT_CACHE_DIR}
    if [[ ! -f ${CACHE_DIR}/${DEPS_FILENAME} ]]; then
      echo "ERROR: missing file ${DEPS_FILENAME} in directory ${CACHE_DIR}"
    fi
    if [[ ! -f ${CACHE_DIR}/${DEV_DEPS_FILENAME} ]]; then
      echo "ERROR: missing file ${DEV_DEPS_FILENAME} in directory ${CACHE_DIR}"
    fi
    echo "${PROJECT_DEPENDENCIES}" > ${PROJECT_CACHE_DIR}/${DEPS_FILENAME}
    echo "${PROJECT_DEV_DEPENDENCIES}" > ${PROJECT_CACHE_DIR}/${DEV_DEPS_FILENAME}

    echo "--- Checking dependencies changes:"
    diff ${CACHE_DIR}/${DEPS_FILENAME} ${PROJECT_CACHE_DIR}/${DEPS_FILENAME}
    [[ $? -ne 0 ]] && exit 1
    echo "--- Checking dev dependencies changes:"
    diff ${CACHE_DIR}/${DEV_DEPS_FILENAME} ${PROJECT_CACHE_DIR}/${DEV_DEPS_FILENAME}
    [[ $? -ne 0 ]] && exit 1
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
