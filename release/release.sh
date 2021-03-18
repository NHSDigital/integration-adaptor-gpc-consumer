#!/bin/bash 

set -e

source ./version.sh

cd ../docker
docker-compose build gpc-consumer

docker tag local/gpc-consumer:latest nhsdev/nia-gpc-consumer-adaptor:${RELEASE_VERSION}

docker scan --severity high --file ../docker/service/Dockerfile --exclude-base nhsdev/nia-gpc-consumer-adaptor:${RELEASE_VERSION}

if [ "$1" == "-y" ];
then
  echo "Tagging and pushing Docker image and git tag"
  docker push nhsdev/nia-gpc-consumer-adaptor:${RELEASE_VERSION}
  git tag -a ${RELEASE_VERSION} -m "Release ${RELEASE_VERSION}"
  git push origin ${RELEASE_VERSION}
fi
