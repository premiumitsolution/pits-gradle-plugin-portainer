# Premium IT Solution Portainer.io gradle plugin

[![Gradle Plugin Portal](https://img.shields.io/badge/Plugin_Portal-v1.0.8-green.svg)](https://plugins.gradle.org/plugin/com.pits.gradle.plugin.portainer)

## Version History

### 1.0.9

- Добавлена возможность задавать volume 
- Добавлены параметры volume и bindings

Пример: 
```groovy
def volumes = ["test_volume_name" : "/var/example"]

portainerSetting {
    volumes = volumesMap
}
```

Параметр volumes должен быть map с ключом = названию директории контейнера и значением - пустым объектом

В bindings указываем полную строку -v test_volume:/var/example в виде списка
### 1.0.8

- Добавлена возможность задавать права доступа для создаваемого контейнера

### 1.0.7

- Исправлен вылет при пустом RepoTag
- Добавлена возможность задавать restartPolicy - 'always', 'onFailure', 'unlessStopped'

## Enabling the plugin

Add to your `build.gradle`:

```gradle
plugins {
  id "com.pits.gradle.plugin.portainer" version "1.0.8"
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
    restartPolicy = ''
    removeOldImages = false
    containerAccess {
        administratorsOnly = false
        publicAccess = false
        teams = ['development']
    }
}
```

sportainerApiUrl - URL for portainer api, for example: https://repo.yourdomain.ru/api

publishedPorts - port for publish in format: type/containerPort/hostPort;type/containerPort/hostPort. For example: tcp/8080/80, tcp/8081/8888

removeOldImages - if true, the old images will be removed from portainer.io restartPolicy - 'always', 'onFailure', 'unlessStopped'

containerAccess - access level for container

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

## Example config

```gradle
portainerSetting {
    portainerApiUrl = 'https://portainer.domain.com/api/'
    portainerLogin = 'deploy'
    portainerPassword = 'password'
    portainerEndPointName = 'dev.domain.com'
    containerName = 'project-1'
    dockerImageName = 'portainer.domain.com/org/project-1'
    dockerImageTag = '1.0.0'
    registryUrl = 'portainer.domain.com'
    publishedPorts = 'tcp/8007/8007',
    removeOldImages = true
    containerAccess {
        administratorsOnly = false
        publicAccess = false
        teams = ['development']
    }
}
```

## Test
Tested with portainer.io version:

- 2.0.0
- 2.1.1