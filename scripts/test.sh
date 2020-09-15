#!/bin/bash
# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  PROJECT_DIR=$(cd ${SCRIPT_DIR}/.. && pwd)
  export PROJECT_DIR
fi;

# Override with a local file, if any
if [[ -f "${PROJECT_DIR}/.local/env.sh" ]]; then
  echo "Loading environment variables from: '.local/env.sh'"
  source ${PROJECT_DIR}/.local/env.sh
  [[ $? -ne 0 ]] && exit 1
else
  echo "No file '${PROJECT_DIR}/.local/env.sh' found. Will use defaults"
fi

# launch tests
ng test --browsers=ChromeHeadless --watch=false
