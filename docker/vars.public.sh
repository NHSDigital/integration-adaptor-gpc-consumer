#!/usr/bin/env bash

export GPC_CONSUMER_SERVER_PORT="8090"
#export GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL="https://orange.testlab.nhs.uk"
export GPC_CONSUMER_URL="http://localhost:8090"
# Sandbox doesn't match public demonstrator, use provided wiremock instead
#export GPC_CONSUMER_SDS_URL="https://sandbox.api.service.nhs.uk/spine-directory/"
export GPC_CONSUMER_SDS_URL="http://wiremock:8080/spine-directory/"