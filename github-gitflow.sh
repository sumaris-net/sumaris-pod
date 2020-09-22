#!/bin/bash

### Control that the script is run on `dev` branch
branch=`git rev-parse --abbrev-ref HEAD`
if [[ ! "$branch" = "release/$2" ]];
then
  echo ">> This script must be run under a release branch (release/$2)"
  exit 1
fi

### Get version to release
current=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`
if [[ "_$current" == "_" ]]; then
  echo "ERROR: Unable to read 'version' in the file 'pom.xml'."
  echo " - Make sure the file 'pom.xml' exists and is readable."
  exit 1
fi
echo "Current version: $current"

### Get repo URL
PROJECT_NAME=sumaris-pod
REMOTE_URL=`git remote -v | grep -P "push" | grep -oP "(https:\/\/github.com\/|git@github.com:)[^ ]+"`
REPO=`echo $REMOTE_URL | sed "s/https:\/\/github.com\///g" | sed "s/git@github.com://g" | sed "s/.git$//"`
REPO_API_URL=https://api.github.com/repos/$REPO
REPO_PUBLIC_URL=https://github.com/$REPO

###  get auth token
GITHUB_TOKEN=`cat ~/.config/${PROJECT_NAME}/.github`
if [[ "_$GITHUB_TOKEN" != "_" ]]; then
    GITHUT_AUTH="Authorization: token $GITHUB_TOKEN"
else
    echo "ERROR: Unable to find github authentication token file: "
    echo " - You can create such a token at https://github.com/settings/tokens > 'Generate a new token'."
    echo " - Then copy the token and paste it in the file '~/.config/${PROJECT_NAME}/.github' using a valid token."
    exit 1
fi

case "$1" in
  del)
    result=`curl -i "$REPO_API_URL/releases/tags/$current"`
    release_url=`echo "$result" | grep -P "\"url\": \"[^\"]+"  | grep -oP "$REPO_API_URL/releases/\d+"`
    if [[ $release_url != "" ]]; then
        echo "Deleting existing release..."
        curl -H  ''"$GITHUT_AUTH"'' -XDELETE $release_url
    fi
  ;;

  pre|rel)

    if [[ $1 = "pre" ]]; then
      prerelease="true"
    else
      prerelease="false"
    fi

    description=`echo $3`
    if [[ "_$description" = "_" ]]; then
        description="Release $current"
    fi

    result=`curl -s -H ''"$GITHUT_AUTH"'' "$REPO_API_URL/releases/tags/$current"`
    release_url=`echo "$result" | grep -P "\"url\": \"[^\"]+" | grep -oP "https://[A-Za-z0-9/.-]+/releases/\d+"`
    if [[ "_$release_url" != "_" ]]; then
        echo "Deleting existing release... $release_url"
        result=`curl -H ''"$GITHUT_AUTH"'' -s -XDELETE $release_url`
        if [[ "_$result" != "_" ]]; then
            error_message=`echo "$result" | grep -P "\"message\": \"[^\"]+" | grep -oP ": \"[^\"]+\""`
            echo "Delete existing release failed with error $error_message"
            exit 1
        fi
    else
        echo "Release not exists yet on github."
    fi

    echo "Creating new release..."
    echo " - tag: $current"
    echo " - description: $description"
    result=`curl -H ''"$GITHUT_AUTH"'' -s $REPO_API_URL/releases -d '{"tag_name": "'"$current"'","target_commitish": "master","name": "'"$current"'","body": "'"$description"'","draft": false,"prerelease": '"$prerelease"'}'`
    upload_url=`echo "$result" | grep -P "\"upload_url\": \"[^\"]+"  | grep -oP "https://[A-Za-z0-9/.-]+"`

    if [[ "_$upload_url" = "_" ]]; then
      echo "Failed to create new release for repo $REPO."
      echo "Server response:"
      echo "$result"
      exit 1
    fi

    ###  Sending artifacts
    echo "Uploading files to $upload_url ..."
    dirname=`pwd`

    JAR_FILE="$dirname/sumaris-server/target/sumaris-server-$current.jar"
    if [[ ! -f ${JAR_FILE} ]]; then
      echo "ERROR: Missing JAR artifact: ${JAR_FILE}. Skipping uppload"
      missing_file=true
    else
      artifact_name="sumaris-pod-$current.jar"
      result=$(curl -s -H ''"$GITHUT_AUTH"'' -H 'Content-Type: application/zip' -T "${JAR_FILE}" "${upload_url}?name=${artifact_name}")
      browser_download_url=`echo "$result" | grep -P "\"browser_download_url\":[ ]?\"[^\"]+" | grep -oP "\"browser_download_url\":[ ]?\"[^\"]+"  | grep -oP "https://[A-Za-z0-9/.-]+"`
      JAR_SHA256=$(sha256sum "${JAR_FILE}" | sed 's/ /\n/gi' | head -n 1)
      echo " - $browser_download_url  | SHA256: ${JAR_SHA256}"

      echo "${JAR_SHA256}  ${artifact_name}" > ${JAR_FILE}.sha256
      result=$(curl -s -H ''"$GITHUT_AUTH"'' -H 'Content-Type: text/plain' -T "${JAR_FILE}.sha256" "${upload_url}?name=${artifact_name}.sha256")
    fi

    DB_FILE="$dirname/sumaris-core/target/sumaris-db-$current.zip"
    if [[ ! -f "${DB_FILE}" ]]; then
      echo "ERROR: Missing DB ZIP artifact: ${DB_FILE}. Skipping uppload"
      missing_file=true
    else
      artifact_name="sumaris-db-$current.zip"
      result=$(curl -s -H ''"$GITHUT_AUTH"'' -H 'Content-Type: application/zip' -T "${DB_FILE}" "${upload_url}?name=${artifact_name}")
      browser_download_url=`echo "$result" | grep -P "\"browser_download_url\":[ ]?\"[^\"]+" | grep -oP "\"browser_download_url\":[ ]?\"[^\"]+"  | grep -oP "https://[A-Za-z0-9/.-]+"`
      DB_SHA256=$(sha256sum "${DB_FILE}" | sed 's/ /\n/gi' | head -n 1)
      echo " - $browser_download_url  | SHA256: ${DB_SHA256}"

      echo "${DB_SHA256}  ${artifact_name}" > ${DB_FILE}.sha256
      result=$(curl -s -H ''"$GITHUT_AUTH"'' -H 'Content-Type: text/plain' -T "${DB_FILE}.sha256" "${upload_url}?name=${artifact_name}.sha256")
    fi

    if [[ ${missing_file} == true ]]; then
      echo "-----------------------------------------"
      echo "ERROR: missing some artifacts (see logs)"
      echo " -> Release url: $REPO_PUBLIC_URL/releases/tag/$current"
      exit 1
    else
      echo "-----------------------------------------"
      echo "Successfully uploading files !"
      echo " -> Release url: $REPO_PUBLIC_URL/releases/tag/$current"
      exit 0
    fi

    ;;
  *)
    echo "Missing arguments"
    echo "Usage:"
    echo " > ./github.sh del|pre|rel <release_description>"
    echo "With:"
    echo " - del: delete existing release"
    echo " - pre: use for pre-release"
    echo " - rel: for full release"
    exit 1
    ;;
esac
