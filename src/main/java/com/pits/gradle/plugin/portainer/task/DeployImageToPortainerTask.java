package com.pits.gradle.plugin.portainer.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pits.gradle.plugin.data.docker.dto.EndpointIPAMConfig;
import com.pits.gradle.plugin.data.docker.dto.EndpointSettings;
import com.pits.gradle.plugin.data.docker.dto.HostConfig;
import com.pits.gradle.plugin.data.docker.dto.Image;
import com.pits.gradle.plugin.data.docker.dto.ImageDeleteResponseItem;
import com.pits.gradle.plugin.data.docker.dto.NetworkingConfig;
import com.pits.gradle.plugin.data.docker.dto.PortBinding;
import com.pits.gradle.plugin.data.docker.dto.RestartPolicy;
import com.pits.gradle.plugin.data.docker.dto.RestartPolicy.NameEnum;
import com.pits.gradle.plugin.data.portainer.ApiClient;
import com.pits.gradle.plugin.data.portainer.ApiException;
import com.pits.gradle.plugin.data.portainer.controller.AuthApi;
import com.pits.gradle.plugin.data.portainer.controller.ContainerApi;
import com.pits.gradle.plugin.data.portainer.controller.EndpointsApi;
import com.pits.gradle.plugin.data.portainer.controller.ResourceControlsApi;
import com.pits.gradle.plugin.data.portainer.controller.TeamsApi;
import com.pits.gradle.plugin.data.portainer.dto.AuthenticateUserRequest;
import com.pits.gradle.plugin.data.portainer.dto.AuthenticateUserResponse;
import com.pits.gradle.plugin.data.portainer.dto.ContainerSummary;
import com.pits.gradle.plugin.data.portainer.dto.EndpointSubset;
import com.pits.gradle.plugin.data.portainer.dto.RegistrySubset;
import com.pits.gradle.plugin.data.portainer.dto.ResourceControlUpdateRequest;
import com.pits.gradle.plugin.data.portainer.dto.Team;
import com.pits.gradle.plugin.portainer.api.PortainerExtendedApi;
import com.pits.gradle.plugin.portainer.data.dto.docker.ContainerCreatePortainerRequest;
import com.pits.gradle.plugin.portainer.data.dto.docker.ContainerCreatePortainerResponse;
import com.pits.gradle.plugin.portainer.setting.ContainerAccessSetting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
public abstract class DeployImageToPortainerTask extends DefaultTask {

  private ApiClient apiClient;
  private PortainerExtendedApi portainerExtendedApi;

  @Input
  abstract public Property<String> getPortainerApiUrl();

  @Input
  abstract public Property<String> getPortainerLogin();

  @Input
  abstract public Property<String> getPortainerPassword();

  @Input
  abstract public Property<String> getDockerImageName();

  @Input
  abstract public Property<String> getDockerImageTag();

  @Input
  abstract public Property<String> getPortainerEndPointName();

  @Input
  abstract public Property<String> getContainerName();

  @Input
  abstract public Property<String> getRegistryUrl();

  @Input
  abstract public Property<String> getPublishedPorts();

  @Input
  abstract public Property<Boolean> getRemoveOldImages();

  @Input
  abstract public Property<String> getRestartPolicy();

  @Input
  abstract public Property<ContainerAccessSetting> getContainerAccess();

  @Input
  abstract public MapProperty<String, String> getVolumes();


  private void initDockerApi() {
    log.info("Initialize initDockerApi");
    Gson gson = new GsonBuilder()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        .create();

    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(Level.BODY);
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.addInterceptor(loggingInterceptor);
    builder.readTimeout(2, TimeUnit.MINUTES);
    builder.writeTimeout(2, TimeUnit.MINUTES);
    builder.hostnameVerifier((hostname, session) -> true);
    OkHttpClient okHttpClient = builder.build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(getPortainerApiUrl().get())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build();
    portainerExtendedApi = retrofit.create(PortainerExtendedApi.class);
  }

  @TaskAction
  public void deployImage() throws Exception {
    try {
      initDockerApi();

      log.info("Deploy image '{}' to portainer '{}' with endpoint '{}'", getPortainerApiUrl().get(), getDockerImageName().get(),
          getPortainerEndPointName().get());

      log.info("Initialize portainerApi");
      init();

      log.info("Authenticate portainerApi");
      String apiToken = authenticate();

      log.info("Determine endpoint");
      Integer endPointId = determineEndPoint();

      log.info("Remove old container");
      removeOldContainer(apiToken, endPointId);

      log.info("Pull image container");
      pullImage(apiToken, endPointId);

      log.info("Create new container with specified image");
      String containerId = createNewContainer(apiToken, endPointId);

      log.info("Start new container with specified image");
      startContainer(apiToken, endPointId, containerId);

      if (getRemoveOldImages().get()) {
        log.info("Remove old images");
        removeOldImages(apiToken, endPointId);
      }
    } catch (ApiException error) {
      log.error("Error: '" + error.getMessage() + "' with response:" + error.getResponseBody(), error);
      throw new Exception(error);
    } catch (Exception error) {
      log.error(error.getMessage(), error);
      throw new Exception(error);
    }
  }

  private void removeOldImages(String apiToken, Integer endPointId) throws IOException {
    Call<List<Image>> imageListCall = portainerExtendedApi.getImageList(endPointId, false, apiToken);
    Response<List<Image>> imageListCallResponse = imageListCall.execute();
    if (imageListCallResponse.code() == 200) {
      List<Image> oldImages = imageListCallResponse.body().stream()
          .filter(image -> image.getRepoTags() != null
              && image.getRepoTags().stream().filter(imageTagValue -> imageTagValue.contains(getDockerImageName().get())).count() > 0)
          .collect(Collectors.toList());

      for (Image imageInfo : oldImages) {
        Call<Image> imageInfoCall = portainerExtendedApi.getImageInfo(endPointId, imageInfo.getId(), apiToken);
        Response<Image> imageInfoCallResponse = imageInfoCall.execute();
        if (imageInfoCallResponse.code() == 200) {
          Image imageDetail = imageInfoCallResponse.body();
          if ((imageDetail.getContainer() != null) && (!imageDetail.getContainer().equals("")) && (!imageDetail.getRepoTags().get(0)
              .contains(String.format("%s:%s", getDockerImageName().get(), getDockerImageTag().get())))) {
            //Remove container
            log.info("Remove image with id='{}'", imageDetail.getId());
            Call<List<ImageDeleteResponseItem>> removeImageCall = portainerExtendedApi.removeImage(endPointId, imageDetail.getId(), apiToken);
            Response<List<ImageDeleteResponseItem>> removeImageCallResponse = removeImageCall.execute();
            if (removeImageCallResponse.code() != 200) {
              throw new RuntimeException("Error remove image:" + removeImageCallResponse.message());
            }
          }
        } else {
          throw new RuntimeException("Error get image info:" + imageInfoCallResponse.message());
        }
      }
    } else {
      throw new RuntimeException("Error while get images list:" + imageListCallResponse.message());
    }
  }

  private void startContainer(String apiToken, Integer endPointId, String containerId) throws IOException {
    Call<Void> callCreate = portainerExtendedApi.startContainer(endPointId, containerId, apiToken);
    Response<Void> responseCreate = callCreate.execute();
    if (responseCreate.code() != 204) {
      throw new RuntimeException("Error while start container:" + responseCreate.message());
    }
    log.info("Started new container with id='{}'", containerId);
  }

  private String createNewContainer(String apiToken, Integer endPointId) throws IOException, ApiException {
    ContainerCreatePortainerRequest containerConfig = new ContainerCreatePortainerRequest();
    containerConfig.image(String.format("%s:%s", getDockerImageName().get(), getDockerImageTag().get()));
    containerConfig.openStdin(false);
    containerConfig.tty(false);
    containerConfig.volumes(createMapOfVolumes());

    RestartPolicy restartPolicy;
    switch (getRestartPolicy().get()) {
      case "onFailure":
        restartPolicy = new RestartPolicy().name(NameEnum.ON_FAILURE);
        break;
      case "unlessStopped":
        restartPolicy = new RestartPolicy().name(NameEnum.UNLESS_STOPPED);
        break;
      case "always":
      default:
        restartPolicy = new RestartPolicy().name(NameEnum.ALWAYS);
        break;
    }

    HostConfig hostConfig = new HostConfig()
        .networkMode("bridge")
        .restartPolicy(restartPolicy)
        .publishAllPorts(false)
        .autoRemove(false)
        .privileged(false)
        .init(false);

    String ports = getPublishedPorts().getOrNull();
    if (ports != null) {
      String[] portArray = ports.split(",");
      if (portArray.length > 0) {
        Map<String, Object> exposedPorts = new HashMap<>();
        Map<String, List<PortBinding>> hostPortBindings = new HashMap<>();
        Arrays.stream(portArray).map(s -> s.split("/")).forEach(strings -> {
          String protocol = strings[0].trim();
          String containerPort = strings[1].trim();
          String hostPort = strings[2].trim();

          // Add exposed port
          String exposeValue = String.format("%s/%s", containerPort, protocol);
          exposedPorts.put(exposeValue, new Object());

          // Add port binding
          hostPortBindings.put(exposeValue, Collections.singletonList(new PortBinding().hostPort(hostPort)));
        });
        hostConfig.portBindings(hostPortBindings);
        setupVolumeBinds(hostConfig);
        containerConfig.setExposedPorts(exposedPorts);
        containerConfig.setHostConfig(hostConfig);

        Map<String, EndpointSettings> endpointSettingsMap = new HashMap<>();
        endpointSettingsMap.put("bridge", new EndpointSettings().ipAMConfig(new EndpointIPAMConfig().ipv4Address("").ipv6Address("")));
        NetworkingConfig networkingConfig = new NetworkingConfig().endpointsConfig(endpointSettingsMap);

        containerConfig.setNetworkingConfig(networkingConfig);
      }
    }

    Call<ContainerCreatePortainerResponse> callDeleteContainer = portainerExtendedApi
        .createContainer(endPointId, containerConfig, getContainerName().get(), apiToken);
    Response<ContainerCreatePortainerResponse> dockerResponse = callDeleteContainer.execute();
    if ((dockerResponse.code() == 200) || (dockerResponse.code() == 201)) {
      ContainerCreatePortainerResponse createResponse = dockerResponse.body();
      StringJoiner sb = new StringJoiner("\n");
      if (createResponse.getWarnings() != null) {
        createResponse.getWarnings().forEach(sb::add);
      }
      log.info("Created new container with id='{}', resourceId={}, warnings='{}'", createResponse.getId(),
          createResponse.getPortainer().getResourceControl().getId(), sb);

      //Установка прав доступа к созданному контейнеру
      setupContainerSecurity(createResponse);

      return createResponse.getId();
    } else {
      throw new RuntimeException("Error while create container:" + dockerResponse.message());
    }
  }

  /**
   * Берем заданные пользователем volumes, берем из них директорию и создаем map
   * где ключ - директория контейнера, а значение - null (так надо)
   * @return
   */
  @NotNull
  private Map<String, Object> createMapOfVolumes() {
    Map<String, String> volumesMap = getVolumes().get();
    Collection<String> volumeDirs = volumesMap.values();
    Map<String, Object> resultVolumesMap = new HashMap<>();
    for (String volumeDir : volumeDirs) {
      resultVolumesMap.put(volumeDir, null);
    }
    return resultVolumesMap;
  }

  /**
   * Устанавливает заданные volumes в binds указанного host config-а
   * @param hostConfig конфиг, в который необходимо установить значение
   */
  private void setupVolumeBinds(HostConfig hostConfig) {
    final Map<String, String> volumesMap = getVolumes().get();

    if (volumesMap.isEmpty()) {
      return;
    }

    List<String> volumesList = volumesMap
            .entrySet()
            .stream()
            .map(entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    final List<String> binds = hostConfig.getBinds();

    if (binds == null) {
      hostConfig.binds(volumesList);
    } else {
      binds.addAll(volumesList);
    }
  }

  private void setupContainerSecurity(ContainerCreatePortainerResponse createResponse) throws ApiException {
    Integer resourceControlId = createResponse.getPortainer().getResourceControl().getId();
    List<Team> portainerTeamList = getPortainerTeamList();
    Map<String, Integer> teamMap = portainerTeamList.stream().collect(Collectors.toMap(Team::getName, Team::getId));
    ResourceControlsApi resourceControlsApi = new ResourceControlsApi(apiClient);
    ContainerAccessSetting containerAccessSetting = getContainerAccess().get();
    ResourceControlUpdateRequest updateData = new ResourceControlUpdateRequest()
        ._public(containerAccessSetting.getPublicAccess().get());
    containerAccessSetting.getTeams().get().forEach(teamCode -> {
      if (teamMap.containsKey(teamCode)) {
        updateData.addTeamsItem(teamMap.get(teamCode));
      } else {
        log.error("Can't found team by code:'{}'", teamCode);
      }
    });
    resourceControlsApi.resourceControlUpdate(resourceControlId, updateData);
  }

  private List<Team> getPortainerTeamList() throws ApiException {
    TeamsApi teamsApi = new TeamsApi(apiClient);
    return teamsApi.teamList();
  }

  private void pullImage(String apiToken, Integer endPointId) throws ApiException, IOException {
    Call<List<RegistrySubset>> registryCall = portainerExtendedApi.getEndpointRegistries(endPointId, apiToken);
    Response<List<RegistrySubset>> registryCallResponse = registryCall.execute();
    if (registryCallResponse.code() == 200) {
      Optional<RegistrySubset> registryOptional = registryCallResponse.body().stream().peek(registrySubset -> {
        log.info("Founded registry: {}", registrySubset.getURL());
      }).filter(registrySubset -> registrySubset.getURL().equals(getRegistryUrl().get())).findFirst();
      if (registryOptional.isPresent()) {
        String registryAuth = String.format("{\n"
            + "  \"registryId\": %s"
            + "}", registryOptional.get().getId());
        registryAuth = Base64.getEncoder().encodeToString(registryAuth.getBytes(StandardCharsets.UTF_8));
        Response<Void> createImageResponse = portainerExtendedApi
            .createImage(endPointId, String.format("%s:%s", getDockerImageName().get(), getDockerImageTag().get()), registryAuth, apiToken)
            .execute();
        if (createImageResponse.code() != 200) {
          throw new RuntimeException("Error while pull container:" + createImageResponse.message());
        }
      } else {
        throw new RuntimeException("Can't found registry by URL:" + getRegistryUrl().get());
      }
    } else {
      throw new RuntimeException(String.format("Can't get registry list for endpoint '%s' : %s", endPointId, registryCallResponse.message()));
    }
  }

  private void init() {
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(Level.BODY);
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.addInterceptor(loggingInterceptor);

    apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(getPortainerApiUrl().get().substring(0, getPortainerApiUrl().get().length() - 1));
    apiClient.setUserAgent("");
  }

  private String authenticate() throws ApiException {
    AuthApi authApi = new AuthApi(apiClient);
    AuthenticateUserRequest authenticateUserRequest = new AuthenticateUserRequest()
        .username(getPortainerLogin().get())
        .password(getPortainerPassword().get());

    AuthenticateUserResponse response = authApi.authenticateUser(authenticateUserRequest);
    apiClient.addDefaultHeader("Authorization", "Bearer " + response.getJwt());
    return response.getJwt();
  }

  private Integer determineEndPoint() throws ApiException {
    EndpointsApi endpointsApi = new EndpointsApi(apiClient);
    List<EndpointSubset> endpointList = endpointsApi.endpointList();
    Optional<EndpointSubset> endpointSubsetOptional = endpointList.stream()
        .filter(endpointSubset -> endpointSubset.getName() != null && endpointSubset.getName().equals(getPortainerEndPointName().get())).findFirst();
    return endpointSubsetOptional.orElseThrow(() -> new ApiException("Can't found endpoint by name:" + getPortainerEndPointName().get())).getId();
  }

  private void removeOldContainer(String apiToken, Integer endpointId) throws Exception {
    ContainerApi containerApi = new ContainerApi(apiClient);
    String fillerByName = String.format("{\"name\": [\"%s\"]}", getContainerName().get());
    List<ContainerSummary> foundedContainers = containerApi.endpointContainerList(endpointId, true, null, null, fillerByName);
    if ((foundedContainers != null) && (foundedContainers.size() > 0)) {
      for (ContainerSummary containerSummary : foundedContainers) {
        log.info("Remove container with id='{}', name='{}' and image='{}'", containerSummary.getId(), containerSummary.getNames(), containerSummary.getImage());
        Call<Void> callDeleteContainer = portainerExtendedApi.removeContainer(endpointId, containerSummary.getId(), true, true, false, apiToken);
        Response<Void> dockerResponse = callDeleteContainer.execute();
        if (dockerResponse.code() != 204) {
          throw new RuntimeException(String.format("Error while delete container '%s': %s", containerSummary.getId(), dockerResponse.message()));
        }
      }
    } else {
      log.info("There is not containers for remove");
    }
  }
}