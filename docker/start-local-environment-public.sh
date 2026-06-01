#!/usr/bin/env bash

set -e

if [ -f "vars.public.sh" ]; then
    source vars.public.sh
else
  echo "No vars.public.sh defined. Using docker-compose defaults."
fi

docker network create commonforgpc
docker-compose down --rmi=local --remove-orphans
docker-compose build
docker-compose up gpc-consumer tkw gpconnect-api gpconnect-db
docker network rm commonforgpc
