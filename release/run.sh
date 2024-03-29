#!/bin/bash 

set -e

source ./version.sh

LIGHT_GREEN='\033[1;32m'
RED='\033[31m'
NC='\033[0m'

echo -e "${LIGHT_GREEN}Exporting environment variables in vars.sh${NC}"
cd ../docker || exit 1
if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "${RED}ERROR: Missing vars.sh file${NC}"
  exit 1
fi

if [[ "$(docker network ls | grep "commonforgpc")" == "" ]] ; then
    docker network create commonforgpc
fi

echo -e "${LIGHT_GREEN}Stopping running containers${NC}"
docker-compose down

if [ "$1" == "-n" ];
then
  echo -e "${RED}Skipping docker image pull for pre-release testing${NC}"
else
  echo -e "${LIGHT_GREEN}Pulling GPC consumer image ${RELEASE_VERSION}${NC}"
  export GPC_CONSUMER_IMAGE="nhsdev/nia-gpc-consumer-adaptor:${RELEASE_VERSION}"
  docker pull "$GPC_CONSUMER_IMAGE"
fi

echo -e "${LIGHT_GREEN}Starting GPC consumer ${RELEASE_VERSION}${NC}"
docker-compose up -d --no-build gpc-consumer

echo -e "${LIGHT_GREEN}Verify all containers are up${NC}"
docker-compose ps