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
    dockerImageName = ''
}
```

portainerApiUrl - URL for prtainder api, for example: https://repo.yourdomain.ru/api

## Tasks

### deployImageToPortainer

Deploy '_dockerImageName_' to endpoint with the name '_portainerEndPointName_':

- Remove containers (search by image name without tags) from the endpoint if it's exists;
- Create container inside the endpoint with specified docker image: '_dockerImageName_';

## Usage

Running plugin:
```sh
# Deploy image to portainer
./gradlew deployImageToPortainer
```