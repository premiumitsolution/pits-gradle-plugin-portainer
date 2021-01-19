package com.pits.gradle.plugin.portainer.task;

import com.pits.gradle.plugin.data.ApiClient;
import com.pits.gradle.plugin.data.ApiException;
import com.pits.gradle.plugin.data.controller.AuthApi;
import com.pits.gradle.plugin.data.controller.EndpointsApi;
import com.pits.gradle.plugin.data.dto.AuthenticateUserRequest;
import com.pits.gradle.plugin.data.dto.AuthenticateUserResponse;
import com.pits.gradle.plugin.data.dto.EndpointSubset;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;


/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
public abstract class DeployImageToPortainerTask extends DefaultTask {

  private ApiClient apiClient;

  @Input
  abstract public Property<String> getPortainerApiUrl();

  @Input
  abstract public Property<String> getPortainerLogin();

  @Input
  abstract public Property<String> getPortainerPassword();

  @Input
  abstract public Property<String> getDockerImageName();

  @Input
  abstract public Property<String> getPortainerEndPointName();

  @TaskAction
  public void deployImage() throws ApiException {
    log.info("Deploy image '{}' to portainer '{}' with endpoint '{}'", getPortainerApiUrl().get(), getDockerImageName().get(),
        getPortainerEndPointName().get());

    log.info("Initialize portainerApi");
    init();

    log.info("Authenticate portainerApi");
    authenticate();

    log.info("Determine endpoint");
    Integer endPointId = determineEndPoint();

    log.info("Remove old container");
    // TODO:

    log.info("Create new container with specified image");
    // TODO:
  }

  private void init() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(getPortainerApiUrl().get());
    apiClient.setUserAgent("");
  }

  private void authenticate() throws ApiException {
    AuthApi authApi = new AuthApi(apiClient);
    AuthenticateUserResponse response = authApi.authenticateUser(new AuthenticateUserRequest()
        .username(getPortainerLogin().get())
        .password(getPortainerPassword().get()));
    apiClient.addDefaultHeader("Authorization", "Bearer " + response.getJwt());
  }

  private Integer determineEndPoint() throws ApiException {
    EndpointsApi endpointsApi = new EndpointsApi(apiClient);
    List<EndpointSubset> endpointList = endpointsApi.endpointList();

    Optional<EndpointSubset> endpointSubsetOptional = endpointList.stream()
        .filter(endpointSubset -> endpointSubset.getName() != null && endpointSubset.getName().equals(getPortainerEndPointName().get())).findFirst();

    return endpointSubsetOptional.orElseThrow(() -> new ApiException("Can't found endpoint by name:" + getPortainerEndPointName().get())).getId();
  }

}
