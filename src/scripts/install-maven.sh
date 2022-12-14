#!/bin/bash

export VER="3.8.6"
INSTALL_DIR="/opt/apache-maven-${VER}"

if [[ -d "/opt/apache-maven-${VER}" && -f "/etc/profile.d/maven.sh" ]]; then
  echo "Apache Maven ${VER} already installed, at '/opt/apache-maven-${VER}'"

  source /etc/profile.d/maven.sh
  exit 0
fi

wget http://www-eu.apache.org/dist/maven/maven-3/${VER}/binaries/apache-maven-${VER}-bin.tar.gz -P /tmp
[[ $? -ne 0 ]] && exit 1

if [[ -e "/opt/maven" ]]; then
  echo "Removing symbolic link '/opt/maven' ..."
  sudo rm /opt/maven
  [[ $? -ne 0 ]] && exit 1
fi

sudo tar xvf apache-maven-${VER}-bin.tar.gz -C /opt
[[ $? -ne 0 ]] && exit 1

sudo ln -s /opt/apache-maven-${VER} /opt/maven
[[ $? -ne 0 ]] && exit 1

if [[ -e "/etc/profile.d/maven.sh" ]]; then
  sudo rm /etc/profile.d/maven.sh
  [[ $? -ne 0 ]] && exit 1
fi

cat <<EOF | sudo tee /etc/profile.d/maven.sh
export JAVA_HOME=/usr/lib/jvm/default-java
export M2_HOME=/opt/maven
export MAVEN_HOME=/opt/maven
export PATH=\${M2_HOME}/bin:\${PATH}
EOF
[[ $? -ne 0 ]] && exit 1

sudo chmod +x /etc/profile.d/maven.sh
[[ $? -ne 0 ]] && exit 1

# Clean tar
sudo rm /tmp/apache-maven-*-bin.tar.gz