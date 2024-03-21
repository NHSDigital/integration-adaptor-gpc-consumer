# National Integration Adaptor - GP Connect Consumer

## Introduction

The GP Connect Consumer Adaptor allows a GP Connect Consumer to connect to a GP Connect Producer over Spine.
The adaptor proxies GP Connect API requests to the correct GP Connect producer. It performs the
 [Spine integration required to consume GP Connect capabilities](https://developer.nhs.uk/apis/gpconnect-1-6-0/integration_illustrated.html#spine-integration-required-to-consume-gp-connect-capabilities).

## Adaptor API

The GP Connect Consumer Adaptor adheres to the GP Connect API specifications.

We only support the two endpoints required for the GP2GP use case:

* Capability: [Migrate Structured Record](https://gpc-structured-1-5-0.netlify.app/accessrecord_structured.html) 
  * Endpoint: [Migrate a patient's structured record](https://developer.nhs.uk/apis/gpconnect-1-6-0/accessrecord_structured_development_migrate_patient_record.html)
* Capability: [Access Document](https://gpc-structured-1-5-0.netlify.app/access_documents.html)
  * Endpoint: [Retrieve a document](https://gpc-structured-1-5-0.netlify.app/access_documents_development_retrieve_patient_documents.html)

### Service Root URL

We follow the [Service Root URL](https://gpc-structured-1-5-0.netlify.app/development_general_api_guidance.html#service-root-url-versioning) scheme defined by GP Connect.

Example (Retrieve a patient's structured record, ODS Code GP0001): `POST https://gpcadaptor.com/GP0001/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord`

### Known Limitations

The adaptor does not perform a PDS lookup/trace. You must perform the PDS lookup before making a request to the adaptor.

## Requirements

* JDK 17
* Docker

## Configuration

The adaptor reads its configuration from environment variables. The following sections describe the environment variables
 used to configure the adaptor.

Variables without a default value and not marked optional, *MUST* be defined for the adaptor to run.

### General Configuration Options

| Environment Variable                        | Default                   | Description
| --------------------------------------------|---------------------------|-------------
| GPC_CONSUMER_SERVER_PORT                    | 8090                      | The port on which the GPC Consumer Adaptor will run.
| GPC_CONSUMER_ROOT_LOGGING_LEVEL             | WARN                      | The logging level applied to the entire application (including third-party dependencies).
| GPC_CONSUMER_LOGGING_LEVEL                  | INFO                      | The logging level applied to GPC Consumer Adaptor components.
| GPC_CONSUMER_LOGGING_FORMAT                 | (*)                       | Defines how to format log events on stdout

Logging level is one of: DEBUG, INFO, WARN, ERROR

The level DEBUG **MUST NOT** be used when handling live patient data.

(*) GP2GP API uses logback (http://logback.qos.ch/). The built-in [logback.xml](service/src/main/resources/logback.xml) 
defines the default log format. This value can be overridden using the `GP2GP_LOGGING_FORMAT` environment variable.
You can provide an external `logback.xml` file using the `-Dlogback.configurationFile` JVM parameter.

### GP Connect API Configuration Options

The adaptor uses the GP Connect API to fetch patient records and documents.

| Environment Variable                        | Default | Description
| --------------------------------------------|---------|-------------
| GPC_CONSUMER_SPINE_CLIENT_CERT              |         | The content of the PEM-formatted client endpoint certificate
| GPC_CONSUMER_SPINE_CLIENT_KEY               |         | The content of the PEM-formatted client private key
| GPC_CONSUMER_SPINE_ROOT_CA_CERT             |         | The content of the PEM-formatted certificate of the issuing Root CA.
| GPC_CONSUMER_SPINE_SUB_CA_CERT              |         | The content of the PEM-formatted certificate of the issuing Sub CA.
| GPC_CONSUMER_SSP_URL                        |         | The URL of Spine Secure Proxy including a trailing slash e.g. https://proxy.opentest.hscic.gov.uk/

### SDS API Configuration Options

You need an [API-M API Key](https://digital.nhs.uk/developer/guides-and-documentation/security-and-authorisation/application-restricted-restful-apis-api-key-authentication)
 for the [SDS FHIR API](https://digital.nhs.uk/developer/api-catalogue/spine-directory-service-fhir) 
to use the adaptor in the integration and production environments.

| Environment Variable    | Default | Description
| ------------------------|---------|-------------
| GPC_CONSUMER_SDS_URL    |         | URL of the SDS FHIR API
| GPC_CONSUMER_SDS_APIKEY |         | Secret key used to authenticate with the API
| GPC_SUPPLIER_ODS_CODE   |         | Supplier ODS code [see GP Connect Docs](https://developer.nhs.uk/apis/gpconnect-1-6-0/integration_spine_directory_service.html#looking-up-a-consumers-own-asid)

### API Configuration Options

**Warning**: We don't recommend overriding these default values. The values are paths of 
[Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway) routes. The defaults conform to the GP Connect 
Service URL scheme.

| Environment Variable                        | Default                                                              | Description
| --------------------------------------------|----------------------------------------------------------------------|-------------
| GPC_CONSUMER_GPC_STRUCTURED_PATH            | /*/STU3/1/gpconnect/structured/fhir/Patient/$gpc.getstructuredrecord | Structured record path.
| GPC_CONSUMER_GPC_GET_DOCUMENT_PATH          | /\*/STU3/1/gpconnect/documents/fhir/Binary/**                        | Get Document record path.
| GPC_CONSUMER_GPC_GET_PATIENT_PATH	          | /*/STU3/1/gpconnect/documents/fhir/Patient                           | Patient record path.
| GPC_CONSUMER_SEARCH_DOCUMENTS_PATH          | /\*/STU3/1/gpconnect/documents/fhir/Patient/**                       | Search for a Patient's Document path.

## How to run service:

The following steps use Docker to provide mocks of adaptor dependencies and infrastructure for local testing and 
development. These containers are not suitable for use in a deployed environment. You are responsible for providing 
adequate infrastructure and connections to external APIs. 

### Copy a configuration example

We provide several example configurations:

* `vars.public.sh` runs the adaptor with the [GP Connect public demonstrator](Docker image - https://developer.nhs.uk/apis/gpconnect-1-6-0/overview_release_notes_1_6_0.html) and the [SDS FHIR API sandbox](https://digital.nhs.uk/developer/guides-and-documentation/testing#sandbox-testing)

```bash
cd docker/
cp vars.opentest.sh vars.sh
```

Edit `vars.sh` to add any missing values e.g. Spine certificates.

### Using the helper script for Docker Compose

For local environment to run against mocks:
```bash
./start-local-environment-mocks.sh
```

For local environment to run against gp demonstrator 1.6.0
```bash
./start-local-environment-public.sh
```

You can also run the docker-compose commands directly.

## How to run tests

**Warning**: Gradle uses a [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) to re-use compile and
test outputs for faster builds. To re-run passing tests without making any code changes you must first run 
`./gradlew clean` to clear the build cache. Otherwise, gradle uses the cached outputs from a previous test execution to 
pass the build.

You must run all gradle commands from the `service/` directory.

### How to run unit tests:

```shell script
./gradlew test
```

### How to run integration tests:

Without special configuration, you must build the gpcc-mocks container using docker-compose before running integration 
tests and after making any changes to the mocks project. The JUnit tests use 
[Testcontainers](https://www.testcontainers.org/) to run the mock service.

```shell script
cd docker/
docker-compose build gpcc-mocks
```

Then run the integration tests from within the IDE or using gradle

```shell script
./gradlew integrationTest
```

It is also possible to run the integration tests without Testcontainers; Setting the `GPC_CONSUMER_SDS_URL` disables
Testcontainers. You must configure the other environment variables also to use the correct services. The integration
tests use the same variables as the application.

### How to run all checks:

```shell script
./gradlew check
```

## Operating the Adaptor

### Dependencies

The stateless GP Connect Consumer Adaptor does not use a database or a message queue.

The adaptor requires an [HSCN](https://digital.nhs.uk/services/health-and-social-care-network) network connection to use the [Spine Secure Proxy](https://developer.nhs.uk/apis/spine-core-1-0/ssp_overview.html).

The adaptor can access the [SDS FHIR API](https://digital.nhs.uk/developer/api-catalogue/spine-directory-service-fhir) over either the HSCN network or the public internet.

### Logging

The adaptor logs to stdout within its container. You must aggregate all logs to store, monitor,
and search them. You must choose a strategy and tooling appropriate for your infrastructure.

The adaptor uses the following log format by default:

```
YYYY-MM-DD HH:MM:SS.mmm Level={DEBUG/INFO/WARNING/ERROR} Logger={LoggerName} RequestId={RequestId} Ssp-TraceID={Ssp-TraceID} Thread="{ThreadName}" Message="{LogMessage}"
```

LoggerName: name of the Java class which emitted the log
RequestId: randomly generated identified for each request
Ssp-TraceID: value of the Ssp-TraceID header, for distributed tracing
ThreadName: name of the thread handling the request
LogMessage: content of the log message

The properties RequestId and Ssp-TraceID may only be populated if the log 
is emitted by application code. These are blank for logs emitted by framework or 
third party code.

## Mapping External (adaptor) URLs to internal (NHSD) URLs

The adaptor proxies its requests to internal NHSD URLs via the Spine Secure Proxy.

```
Example: Get structured record for a patient at practice with ODS code A12345

The request URL made to the adaptor includes the ODS code of the patient's practice. The two 
variables (with * as wildcard) must have values matching the full request URL.

https://gpcconsumer.prod.mydomain.com/A12345/STU3/1/gpconnect/structured/fhir/Patient/$gpc.getstructuredrecord
|            Adaptor's Host          |          $GPC_CONSUMER_GPC_STRUCTURED_PATH                             |

The adaptor then performs an SDS lookup and constructs a new path using the Spine
Secure Proxy and the practice's GP Connect Provider's internal URL.

https://proxy.opentest.hscic.gov.uk/https://gpconnect.gpsytemsupplier.internal.nhs.uk/A12345/STU3/1/gpconnect/structured/fhir/Patient/$gpc.getstructuredrecord
|       $GPC_CONSUMER_SSP_URL     |     [ From SDS Lookup ]                         |     [ Path from original request ]                                      |
```

### Additional Functionality

The adaptor re-writes URLs in response bodies to refer to adaptor URLs instead of GP Connect Provider URLs.

For example (Search for a patient's documents):

When making a "Search for a patient's documents" request

```
GET https://orange.testlab.nhs.uk/B82617/STU3/1/gpconnect/fhir/Patient/2/DocumentReference?...
```

the adaptor replaces the GP Connect Provider Hosts (`https://orange.testlab.nhs.uk/`) in the original response

```
...
        {
            "fullUrl": "https://orange.testlab.nhs.uk/B82617/STU3/1/gpconnect/documents/fhir/DocumentReference/27863182736",
            "resource": {
                "resourceType": "DocumentReference",
                ...
                "content": [
                    {
                        "attachment": {
                            "contentType": "application/msword",
                            "url": "https://orange.testlab.nhs.uk/B82617/STU3/1/gpconnect/documents/fhir/Binary/07a6483f-732b-461e-86b6-edb665c45510",
                            "size": 3654
                        }
                    }
                ],
...
```

with the dns name / port used in the original request.

```
...
        {
            "fullUrl": "http://localhost:8090/B82617/STU3/1/gpconnect/documents/fhir/DocumentReference/27863182736",
            "resource": {
                "resourceType": "DocumentReference",
                ...
                "content": [
                    {
                        "attachment": {
                            "contentType": "application/msword",
                            "url": "http://localhost:8090/B82617/STU3/1/gpconnect/documents/fhir/Binary/07a6483f-732b-461e-86b6-edb665c45510",
                            "size": 3654
                        }
                    }
                ],
...
```

## Example Deployment

We release adaptor image on Dockerhub as [nhsdev/nia-gpc-consumer-adaptor][docker-hub-image],
with the latest changes documented within the [CHANGELOG.MD](/CHANGELOG.MD).

When performing assurance against a simulated workload involving the transfer of 100MB documents, we
have identified a minimum of 2GB of RAM and 2 vCPUs to the container is required.

We provide [Terraform scripts][exemplar-deployment] to perform an exemplar deployment of the GP2GP adaptor
and GP Connect Consumer adaptor into AWS.

[exemplar-deployment]: https://github.com/nhsconnect/integration-adaptors/tree/develop/terraform/aws/components/gp2gp
[docker-hub-image]: https://hub.docker.com/r/nhsdev/nia-gpc-consumer-adaptor

## Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).
