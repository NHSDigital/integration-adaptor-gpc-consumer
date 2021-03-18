# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.1] - 2021-03-17

### Known Issues and Limitations

* Routing via Spine Security Proxy is not yet supported
* Using TLS MA to authenticate GP Connect requests is not yet supported

### Added

- NIAD-1080  Lookup GPC endpoints in SDS Filter
- NIAD-1129  Proxy search for patient documents
- NIAD-1130  Proxy get document
- NIAD-1128  Proxy find a patient
- NIAD-1125  SDS API client
- NIAD-1121  Jenkins pipeline with no deployment
- NIAD-1127  Proxy get structured record
- NIAD-1119  Create initial project structure
- NIAD-1123  Docker Compose files
- NIAD-1119  Create Github and Spring Boot Healthcheck

### Fixed

Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-COMGOOGLEGUAVA-1015415] in com.google.guava:guava@29.0-jre
HTTP Request Smuggling [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1083991] in io.netty:netty-codec-http2@4.1.59.Final
Comparison Using Wrong Factors [High Severity][https://snyk.io/vuln/SNYK-JAVA-ORGBOUNCYCASTLE-1052448] in org.bouncycastle:bcprov-jdk15on@1.66
