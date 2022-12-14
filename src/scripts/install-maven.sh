#!/bin/bash

export MAVEN_VERSION="3.8.6"

if [[ -d "/opt/apache-maven-${MAVEN_VERSION}" && -f "/etc/profile.d/maven.sh" ]]; then
  echo "Apache Maven ${MAVEN_VERSION} already installed, at '/opt/apache-maven-${MAVEN_VERSION}'"

  source /etc/profile.d/maven.sh
  exit 0
fi

wget http://www-eu.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -P /tmp
[[ $? -ne 0 ]] && exit 1

if [[ -e "/opt/maven" ]]; then
  echo "Removing symbolic link '/opt/maven' ..."
  sudo rm /opt/maven
  [[ $? -ne 0 ]] && exit 1
fi

sudo tar xvf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt
[[ $? -ne 0 ]] && exit 1

sudo ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven
[[ $? -ne 0 ]] && exit 1

if [[ -e "/etc/profile.d/maven.sh" ]]; then
  sudo rm /etc/profile.d/maven.sh
  [[ $? -ne 0 ]] && exit 1
fi

cat <<EOF | sudo tee /etc/profile.d/maven.sh
export M2_HOME=/opt/maven
export MAVEN_HOME=/opt/maven
export PATH=\${M2_HOME}/bin:\${PATH}
EOF
[[ $? -ne 0 ]] && exit 1

sudo chmod +x /etc/profile.d/maven.sh
[[ $? -ne 0 ]] && exit 1

# Clean tar
sudo rm /tmp/apache-maven-*-bin.tar.gz