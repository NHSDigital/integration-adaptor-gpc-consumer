# Quick Reference

- **Maintained by:** NHS Digital
- **Where to get help:** https://github.com/nhsconnect/integration-adaptor-gpc-consumer
- **Where to file issues:** https://github.com/nhsconnect/integration-adaptor-gpc-consumer/issues

# What is the GPC Consumer Adaptor?

The GPC Consumer Adaptor proxies requests to GP Connect provider endpoints. The adaptor simplifies your GP Connect integration by performing service discovery via the Spine Directory Service and routing over the Spine Secure Proxy. We only support the interactions required for GP2GP over GP Connect and the [GP2GP Adaptor](https://github.com/nhsconnect/integration-adaptor-gp2gp).

# How to Use This Image

To help you begin using the GPC Consumer Adaptor, we provide shell scripts and Docker Compose configurations.

## Clone the Repository

```bash
git clone https://github.com/nhsconnect/integration-adaptor-gpc-consumer.git
```

## Pull the Latest Changes and Checkout the Release Tag

Every tagged container on Docker Hub has a corresponding tag in the git repository. Checkout the tag of the release
you are testing to ensure compatibility with configurations and scripts.

```bash
git pull
git checkout 0.3.3
```

## Find the Docker Directory

```bash
cd integration-adaptor-gpc-consumer/docker
```

## Configure the Application

[Copy a configuration example](https://github.com/nhsconnect/integration-adaptor-gpc-consumer#copy-a-configuration-example)
to `docker/vars.sh` and make any required changes to the `vars.sh` file.

## Find the Release Directory

```bash
cd ../release
```

## Start the Adaptor

The script pulls the released GPC Consumer Adaptor container image from Docker Hub. It builds containers for its dependencies
from the Dockerfiles in the repository.

```bash
./run.sh
```

## Monitor the Logs

```bash
./logs.sh
```

## Run the Tests

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

## Stopping the Adaptor

```bash
cd ../docker
docker-compose down
```
