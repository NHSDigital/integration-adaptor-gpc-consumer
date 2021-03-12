#!/usr/bin/env bash

export GPC_CONSUMER_SERVER_PORT="8080"
export GPC_CONSUMER_GPC_GET_URL="http://localhost:8210"
export GPC_CONSUMER_GPC_STRUCTURED_PATH="/GP0001/STU3/1/gpconnect/fhir/Patient/\$gpc.getstructuredrecord"
export GPC_CONSUMER_GPC_GET_DOCUMENT_PATH="/GP0001/STU3/1/gpconnect/fhir/Binary/{documentId}"
export GPC_CONSUMER_GPC_GET_PATIENT_PATH="/GP0001/STU3/1/gpconnect/fhir/Patient"
export GPC_CONSUMER_URL="http://localhost:8080"
export GPC_CONSUMER_SEARCH_DOCUMENTS_PATH="/GP0001/STU3/1/gpconnect/fhir/Patient/**"
