package com.pits.gradle.plugin.portainer.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
import retrofit2.converter.jackson.JacksonConverterFactory;


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


  private void initDockerApi() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .client(okHttpClient)
        .build();
    portainerDockerApi = retrofit.create(PortainerDockerApi.class);
  }

  @TaskAction
  public void deployImage() throws Exception {
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
    // TODO:
  }

  private void pullImage(String apiToken, Integer endPointId) throws IOException {
    String registryAuth = String.format("{\n"
        + "  \"serveraddress\": \"%s\""
        + "}", getRegistryUrl());
    registryAuth = Base64.getEncoder().encodeToString(registryAuth.getBytes(StandardCharsets.UTF_8));
    Response<Void> createImageResponse = portainerDockerApi
        .createImage(endPointId, String.format("%s:%s", getDockerImageName(), getDockerImageTag()), registryAuth, apiToken)
        .execute();
    if (createImageResponse.code() != 200) {
      throw new RuntimeException("Error while pull container:" + createImageResponse.message());
    }
  }

  private void init() {
    apiClient = new ApiClient();
    apiClient.setBasePath(getPortainerApiUrl().get());
    apiClient.setUserAgent("");
  }

  private String authenticate() throws ApiException {
    AuthApi authApi = new AuthApi(apiClient);
    AuthenticateUserResponse response = authApi.authenticateUser(new AuthenticateUserRequest()
        .username(getPortainerLogin().get())
        .password(getPortainerPassword().get()));
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
