#!/usr/bin/env bash

set -e

if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "No vars.sh defined. Using docker-compose defaults."
fi

docker network create commonforgpc
docker-compose down --rmi=local --remove-orphans
docker-compose build
docker-compose up gpc-consumer gpcc-mocks 
docker network rm commonforgpc
