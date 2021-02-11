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
    publishedPorts = ''
}
```

portainerApiUrl - URL for portainer api, for example: https://repo.yourdomain.ru/api
publishedPorts - port for publish in format: type/containerPort/hostPort;type/containerPort/hostPort. For exmaple: tcp/8080/80, tcp/8081/88

## Tasks

### deployImageToPortainer

Deploy '_dockerImageName_':'_dockerImageTag_' to endpoint with the name '_portainerEndPointName_':

- Remove container (search by '_containerName_') from the endpoint if it's exists;
- Pull image '_dockerImageName_':'_dockerImageTag_' from '_dockerImageTag_';
- Create container inside the endpoint with specified docker image: '_dockerImageName_':'_dockerImageTag_' and container name: '_containerName_'. If set _
  publishedPorts_' the port will be published inside containers;
- Start container with container name: '_containerName_'.

## Usage

To run the plugin execute next command:

```sh
# Deploy image to portainer
./gradle deployImageToPortainer
```

## Test

Testing with portainer.io version:

- 2.0.0
- 2.1.1