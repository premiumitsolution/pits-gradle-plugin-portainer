package com.pits.gradle.plugin.portainer.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pits.gradle.plugin.data.docker.dto.ContainerCreateResponse;
import com.pits.gradle.plugin.data.docker.dto.EndpointIPAMConfig;
import com.pits.gradle.plugin.data.docker.dto.EndpointSettings;
import com.pits.gradle.plugin.data.docker.dto.HostConfig;
import com.pits.gradle.plugin.data.docker.dto.NetworkingConfig;
import com.pits.gradle.plugin.data.docker.dto.PortBinding;
import com.pits.gradle.plugin.data.docker.dto.RestartPolicy;
import com.pits.gradle.plugin.data.docker.dto.RestartPolicy.NameEnum;
import com.pits.gradle.plugin.data.portainer.ApiClient;
import com.pits.gradle.plugin.data.portainer.ApiException;
import com.pits.gradle.plugin.data.portainer.controller.AuthApi;
import com.pits.gradle.plugin.data.portainer.controller.ContainerApi;
import com.pits.gradle.plugin.data.portainer.controller.EndpointsApi;
import com.pits.gradle.plugin.data.portainer.dto.AuthenticateUserRequest;
import com.pits.gradle.plugin.data.portainer.dto.AuthenticateUserResponse;
import com.pits.gradle.plugin.data.portainer.dto.ContainerSummary;
import com.pits.gradle.plugin.data.portainer.dto.EndpointSubset;
import com.pits.gradle.plugin.portainer.api.PortainerDockerApi;
import com.pits.gradle.plugin.portainer.data.dto.docker.ContainerCreatePortainerRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
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
  private PortainerDockerApi portainerDockerApi;

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
    portainerDockerApi = retrofit.create(PortainerDockerApi.class);
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
    } catch (ApiException error) {
      log.error("Error: '" + error.getMessage() + "' with response:" + error.getResponseBody(), error);
      throw new Exception(error.getMessage());
    } catch (Exception error) {
      log.error(error.getMessage(), error);
    }
  }

  private void startContainer(String apiToken, Integer endPointId, String containerId) throws IOException {
    Call<Void> callCreate = portainerDockerApi.startContainer(endPointId, containerId, apiToken);
    Response<Void> responseCreate = callCreate.execute();
    if (responseCreate.code() != 204) {
      throw new RuntimeException("Error while start container:" + responseCreate.message());
    }
    log.info("Started new container with id='{}'", containerId);
  }

  private String createNewContainer(String apiToken, Integer endPointId) throws IOException {
    ContainerCreatePortainerRequest containerConfig = new ContainerCreatePortainerRequest();
    containerConfig.image(String.format("%s:%s", getDockerImageName().get(), getDockerImageTag().get()));
    containerConfig.openStdin(false);
    containerConfig.tty(false);

    String ports = getPublishedPorts().getOrNull();
    if (ports != null) {
      String[] portArray = ports.split(",");
      if (portArray.length > 0) {
        Map<String, Object> exposedPorts = new HashMap<>();
        Map<String, List<PortBinding>> hostPortBindings = new HashMap<>();

        HostConfig hostConfig = new HostConfig()
            .networkMode("bridge")
            .restartPolicy(new RestartPolicy().name(NameEnum.EMPTY))
            .publishAllPorts(false)
            .autoRemove(false)
            .privileged(false)
            .init(false);

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
        containerConfig.setExposedPorts(exposedPorts);
        containerConfig.setHostConfig(hostConfig);

        Map<String, EndpointSettings> endpointSettingsMap = new HashMap<>();
        endpointSettingsMap.put("bridge", new EndpointSettings().ipAMConfig(new EndpointIPAMConfig().ipv4Address("").ipv6Address("")));
        NetworkingConfig networkingConfig = new NetworkingConfig().endpointsConfig(endpointSettingsMap);

        containerConfig.setNetworkingConfig(networkingConfig);
      }
    }

    Call<ContainerCreateResponse> callDeleteContainer = portainerDockerApi.createContainer(endPointId, containerConfig, getContainerName().get(), apiToken);
    Response<ContainerCreateResponse> dockerResponse = callDeleteContainer.execute();
    if ((dockerResponse.code() == 200) || (dockerResponse.code() == 201)) {
      ContainerCreateResponse createResponse = dockerResponse.body();
      StringJoiner sb = new StringJoiner("\n");
      if (createResponse.getWarnings() != null) {
        createResponse.getWarnings().forEach(sb::add);
      }
      log.info("Created new container with id='{}', warnings='{}'", createResponse.getId(), sb);
      return createResponse.getId();
    } else {
      throw new RuntimeException("Error while create container:" + dockerResponse.message());
    }
  }

  private void pullImage(String apiToken, Integer endPointId) throws IOException {
    String registryAuth = String.format("{\n"
        + "  \"serveraddress\": \"%s\""
        + "}", getRegistryUrl().get());
    registryAuth = Base64.getEncoder().encodeToString(registryAuth.getBytes(StandardCharsets.UTF_8));
    Response<Void> createImageResponse = portainerDockerApi
        .createImage(endPointId, String.format("%s:%s", getDockerImageName().get(), getDockerImageTag().get()), registryAuth, apiToken)
        .execute();
    if (createImageResponse.code() != 200) {
      throw new RuntimeException("Error while pull container:" + createImageResponse.message());
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
        Call<Void> callDeleteContainer = portainerDockerApi.removeContainer(endpointId, containerSummary.getId(), true, true, false, apiToken);
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