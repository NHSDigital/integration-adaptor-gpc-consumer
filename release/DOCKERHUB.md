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
git checkout 0.0.1
```

## Find the docker directory

```bash
cd integration-adaptor-gpc-consumer/docker
```

## Configure the application

The repository includes several configuration examples:
* `vars.local.sh` template to run the adaptor against mock service containers
* `vars.opentest.sh` template to run the adaptor against the OpenTest environment

Configure the application by copying a `vars.*.sh` file to `vars.sh`

```bash
cp vars.local.sh vars.sh
```

Make any required changes to the `vars.sh` file. If using `vars.local.sh` you do not need to modify anything. Refer
to the [README](https://github.com/nhsconnect/integration-adaptor-gpc-consumer/blob/0.0.1/README.md) for possible configuration
options.

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

We provide shell scripts in the release/tests directory to help you start testing.

* `healthcheck.sh` verifies that the adaptor's healthcheck endpoint is available
* `e2e.sh` starts a docker container that runs the adaptor's end-to-end tests

```bash
cd tests/
./healthcheck.sh
```

## Stopping the adaptor
```bash
cd ../docker
docker-compose down
```
