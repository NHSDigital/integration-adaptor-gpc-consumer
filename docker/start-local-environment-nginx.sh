#!/usr/bin/env bash

source vars.local-nginx.sh

docker network create commonforgpc
docker-compose down --rmi=local --remove-orphans
docker-compose build
docker-compose up gpc-consumer gpcc-mocks gpcc-nginx
docker network rm commonforgpc
