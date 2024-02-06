#!/bin/bash 

set -e

source ./version.sh

git fetch
git checkout $RELEASE_VERSION

cd ../

docker buildx build -f docker/service/Dockerfile . --platform linux/arm64/v8,linux/amd64 --tag nhsdev/nia-gpc-consumer-adaptor:${RELEASE_VERSION} --push

docker scout cves --only-severity critical,high --ignore-base nhsdev/nia-gpc-consumer-adaptor:${RELEASE_VERSION}