# Quick reference
- Maintained by: NHS Digital
- Where to get help: https://github.com/nhsconnect/integration-adaptor-gpc-consumer
- Where to file issues: https://github.com/nhsconnect/integration-adaptor-gpc-consumer/issues

# What is the GPC Consumer?
The GPC Consumer adaptor proxies requests to GP Connect provider endpoints. The adaptor simplifies your GP Connect integration by performing service discovery via Spine Directory Service and routing over Spine Security Proxy. We only support the interactions required for GP2GP over GP Connect and the [GP2GP Adaptor](https://github.com/nhsconnect/integration-adaptor-gp2gp).

# How to use this image

To help you begin using the GPC Consumer we provide shell scripts and Docker Compose configurations.

## Clone the repository

```bash
git clone https://github.com/nhsconnect/integration-adaptor-gpc-consumer.git
```

## Pull the latest changes and checkout the release tag

Every tagged container on Docker hub has a corresponding tag in the git repository. Checkout the tag of the release
you are testing to ensure compatibility with configurations and scripts.

```bash
git pull
git checkout 0.2.5
```

## Find the docker directory

```bash
cd integration-adaptor-gpc-consumer/docker
```

## Configure the application

[Copy a configuration example](https://github.com/nhsconnect/integration-adaptor-gpc-consumer/blob/0.0.5/README.md#copy-a-configuration-example)
 to `docker/vars.sh` and make any required changes to the `vars.sh` file.

## Find the release directory

```bash
cd ../release
```

## Start the adaptor

The script pulls the released GPC Consumer adaptor container image from Docker Hub. It builds containers for its dependencies
from the Dockerfiles in the repository.

```bash
./run.sh
```

## Monitor the logs

```bash
./logs.sh
```

## Run the tests

We provide a shell script to help you determine if the adaptor is running.

* `healthcheck.sh` verifies that the adaptor's healthcheck endpoint is available

```bash
cd tests/
./healthcheck.sh
```

## Test with Postman

You can use the [GP Connect Postman Samples](https://orange.testlab.nhs.uk/index.html#postman-samples) to test the adaptor.

Modify the values for the environment "Public GP Connect Reference Implementation" as follows:

* providerURL_1_5_x_structured: `http://localhost:8090/B82617/STU3/1/gpconnect/structured/fhir`
* providerURL_1_5_x_documents: `http://localhost:8090/B82617/STU3/1/gpconnect/documents/fhir`

## Stopping the adaptor
```bash
cd ../docker
docker-compose down
```
