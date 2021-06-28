#!/usr/bin/env bash
# SDS Sandbox API URLs don't match the public demonstrator. Use the 'local' configuration instead
export GPC_CONSUMER_SERVER_PORT="8090"
export GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL="https://orange.testlab.nhs.uk"
export GPC_CONSUMER_SDS_URL="https://sandbox.api.service.nhs.uk/spine-directory/"
