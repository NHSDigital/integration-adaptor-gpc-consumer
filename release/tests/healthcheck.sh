#!/bin/bash

set -ex

curl -i --location --request GET 'http://localhost:8090/healthcheck'
