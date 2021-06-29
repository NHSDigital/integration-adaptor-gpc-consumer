#!/usr/bin/env bash
export GPC_CONSUMER_SERVER_PORT="8090"
export GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL="http://gpcc-mocks:8080"
export GPC_CONSUMER_SDS_URL="http://gpcc-mocks:8080/spine-directory/"
export GPC_ENABLE_SDS="true"
export GPC_CONSUMER_SDS_APIKEY="anykey"
export GPC_CONSUMER_LOGGING_LEVEL="DEBUG"