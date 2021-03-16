#!/usr/bin/env bash

export GPC_CONSUMER_SERVER_PORT="8080"
export GPC_CONSUMER_GPC_GET_URL="https://orange.testlab.nhs.uk"
export GPC_CONSUMER_GPC_STRUCTURED_PATH="/*/STU3/1/gpconnect/fhir/Patient/\$gpc.getstructuredrecord"
export GPC_CONSUMER_GPC_GET_DOCUMENT_PATH="/*/STU3/1/gpconnect/fhir/Binary/**"
export GPC_CONSUMER_GPC_GET_PATIENT_PATH="/*/STU3/1/gpconnect/fhir/Patient"
export GPC_CONSUMER_URL="http://localhost:8080"
export GPC_CONSUMER_SEARCH_DOCUMENTS_PATH="/*/STU3/1/gpconnect/fhir/Patient/**"
