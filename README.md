# Premium IT Solution Portainer.io gradle plugin

[![Gradle Plugin Portal](https://img.shields.io/badge/Plugin_Portal-v1.0.0-green.svg)](https://plugins.gradle.org/plugin/com.pits.gradle.plugin.portainer)

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
    containerName = ''
    dockerImageName = ''
    dockerImageTag = ''
    registryUrl = ''
}
```

portainerApiUrl - URL for portainer api, for example: https://repo.yourdomain.ru/api

## Tasks

### deployImageToPortainer

Deploy '_dockerImageName_':'_dockerImageTag_' to endpoint with the name '_portainerEndPointName_':

- Remove container (search by '_containerName_') from the endpoint if it's exists;
- Pull image '_dockerImageName_':'_dockerImageTag_' from '_dockerImageTag_';
- Create container inside the endpoint with specified docker image: '_dockerImageName_':'_dockerImageTag_' and container name: '_containerName_';
- Start container with container name: '_containerName_'.

## Usage

Running plugin:
```sh
# Deploy image to portainer
./gradlew deployImageToPortainer
```