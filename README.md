# Portainer.io gradle plugin

[![Build Status](https://github.com/coditory/gradle-integration-test-plugin/workflows/Build/badge.svg?branch=master)](https://github.com/coditory/gradle-integration-test-plugin/actions?query=workflow%3ABuild+branch%3Amaster)
[![Gradle Plugin Portal](https://img.shields.io/badge/Plugin_Portal-v1.1.9-green.svg)](https://plugins.gradle.org/plugin/com.coditory.integration-test)

## Enabling the plugin

Add to your `build.gradle`:

```gradle
plugins {
  id "com.pits.gradle.plugin.portainer" version "1.0.0"
}

portainerSetting {
    portainerApiUrl = ''
    portainerLogin = ''
    portainerPassword = ''
    portainerEndPointName = ''
    dockerImageName = ''
}
```

## Tasks

### deployImageToPortainer
Deploy '_dockerImageName_' to endpoint with the name '_portainerEndPointName_':
- Remove container from the endpoint if it's exists;
- Create container inside the endpoint with specified docker image: '_dockerImageName_';

## Usage

Running tests:
```sh
# Deploy image to portainer
./gradlew deployImageToPortainer
```