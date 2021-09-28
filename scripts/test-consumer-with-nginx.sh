#!/bin/bash

##
echo "Run './start-local-environment-nginx.sh' from the docker folder to start services"
##

curl -k --location --request POST 'https://localhost:8091/B000356/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord' \
--header 'Accept: application/fhir+json' \
--header 'Ssp-From: 200000000359' \
--header 'Ssp-To: 918999198738' \
--header 'Ssp-InteractionID: urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1' \
--header 'Ssp-TraceID: 7e8c547f-f524-4401-97dd-a958ad84f3f4' \
--header 'Content-Type: application/fhir+json' \
--header 'Authorization: Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwOi8vZ3Bjb25uZWN0LXBvc3RtYW4tdXJsIiwic3ViIjoiMSIsImF1ZCI6Imh0dHBzOi8vb3JhbmdlLnRlc3RsYWIubmhzLnVrL0I4MjYxNy9TVFUzLzEvZ3Bjb25uZWN0L2ZoaXIiLCJleHAiOjE2MzI3MzQ0MDYsImlhdCI6MTYzMjczNDEwNiwicmVhc29uX2Zvcl9yZXF1ZXN0IjoiZGlyZWN0Y2FyZSIsInJlcXVlc3RlZF9zY29wZSI6InBhdGllbnQvKi5yZWFkIiwicmVxdWVzdGluZ19kZXZpY2UiOnsicmVzb3VyY2VUeXBlIjoiRGV2aWNlIiwiaWQiOiIxIiwiaWRlbnRpZmllciI6W3sic3lzdGVtIjoiV2ViIEludGVyZmFjZSIsInZhbHVlIjoiUG9zdG1hbiBleGFtcGxlIGNvbnN1bWVyIn1dLCJtb2RlbCI6IlBvc3RtYW4iLCJ2ZXJzaW9uIjoiMS4wIn0sInJlcXVlc3Rpbmdfb3JnYW5pemF0aW9uIjp7InJlc291cmNlVHlwZSI6Ik9yZ2FuaXphdGlvbiIsImlkZW50aWZpZXIiOlt7InN5c3RlbSI6Imh0dHBzOi8vZmhpci5uaHMudWsvSWQvb2RzLW9yZ2FuaXphdGlvbi1jb2RlIiwidmFsdWUiOiJHUEMwMDEifV0sIm5hbWUiOiJQb3N0bWFuIE9yZ2FuaXphdGlvbiJ9LCJyZXF1ZXN0aW5nX3ByYWN0aXRpb25lciI6eyJyZXNvdXJjZVR5cGUiOiJQcmFjdGl0aW9uZXIiLCJpZCI6IjEiLCJpZGVudGlmaWVyIjpbeyJzeXN0ZW0iOiJodHRwczovL2ZoaXIubmhzLnVrL0lkL3Nkcy11c2VyLWlkIiwidmFsdWUiOiJHMTM1NzkxMzUifSx7InN5c3RlbSI6Imh0dHBzOi8vZmhpci5uaHMudWsvSWQvc2RzLXJvbGUtcHJvZmlsZS1pZCIsInZhbHVlIjoiMTExMTExMTExIn1dLCJuYW1lIjpbeyJmYW1pbHkiOiJEZW1vbnN0cmF0b3IiLCJnaXZlbiI6WyJHUENvbm5lY3QiXSwicHJlZml4IjpbIk1yIl19XX19.' \
--data-raw '{
  "resourceType": "Parameters",
  "parameter": [
  {
    "name": "patientNHSNumber",
    "valueIdentifier": {
        "system": "https://fhir.nhs.uk/Id/nhs-number",
        "value": "9690937286"
    }
  },
  {
    "name": "includeAllergies",
    "part": [ {
        "name": "includeResolvedAllergies",
        "valueBoolean": true
    }
    ]
  },
  {
    "name": "includeMedication",
    "part": [
    {
        "name": "includePrescriptionIssues",
        "valueBoolean": true
    },
    {
        "name": "medicationSearchFromDate",
        "valueDate": "1980-06-05"
    }
    ]
  }
  ]}'