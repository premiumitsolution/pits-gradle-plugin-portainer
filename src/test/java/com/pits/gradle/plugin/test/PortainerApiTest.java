package com.pits.gradle.plugin.test;

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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.gradle.internal.impldep.org.apache.http.util.Asserts;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public class PortainerApiTest {

  private PortainerDockerApi initDockerApi() {
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
        .baseUrl("https://port.premiumitsolution.com/api/")
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .client(okHttpClient)
        .build();
    return retrofit.create(PortainerDockerApi.class);
  }

  @Test
  public void testApi() throws Exception {
    String imageName = "registry.premiumitsolution.com/erp/pits-erp-project";
    String imageTag = "1.1.6.24";
    String containerName = "pits-erp-project";

    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(Level.BODY);
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.addInterceptor(loggingInterceptor);

    ApiClient apiClient = new ApiClient(builder.build());
    apiClient.setBasePath("https://port.premiumitsolution.com/api");
    apiClient.setUserAgent("");
    AuthApi authApi = new AuthApi(apiClient);
    AuthenticateUserResponse response = authApi.authenticateUser(new AuthenticateUserRequest()
        .username("deploy")
        .password("34CBrYj5eTg58j4zPj85xYiOeQ0iLtacNXFx4lVrt7g="));
    Asserts.notEmpty(response.getJwt(), "Jwt");

    String apiToken = response.getJwt();

    apiClient.addDefaultHeader("Authorization", "Bearer " + apiToken);

    EndpointsApi endpointsApi = new EndpointsApi(apiClient);
    List<EndpointSubset> endpointList = endpointsApi.endpointList();

    Optional<EndpointSubset> endpointSubsetOptional = endpointList.stream()
        .filter(endpointSubset -> endpointSubset.getName() != null && endpointSubset.getName().equals("dev")).findFirst();

    EndpointSubset endPoint = endpointSubsetOptional.orElseThrow(() -> new ApiException("Can't found endpoint by name: qa"));

    ContainerApi containerApi = new ContainerApi(apiClient);

    String fillerByName = String.format("{\"name\": [\"%s\"]}", containerName);
    List<ContainerSummary> foundedContainers = containerApi.endpointContainerList(endPoint.getId(), true, null, null, fillerByName);

    PortainerDockerApi portainerDockerApi = initDockerApi();

    // Delete container
    if ((foundedContainers != null) && (foundedContainers.size() > 0)) {
      for (ContainerSummary containerSummary : foundedContainers) {
        System.out.printf("Remove container with id='%s', name='%s' and image='%s'%n", containerSummary.getId(), containerSummary.getNames(),
            containerSummary.getImage());
        Call<Void> callDeleteContainer = portainerDockerApi.removeContainer(endPoint.getId(), containerSummary.getId(), true, true, false, apiToken);
        Response<Void> dockerResponse = callDeleteContainer.execute();
        if (dockerResponse.code() != 204) {
          throw new RuntimeException("Error while delete container:" + dockerResponse.message());
        }
      }
    } else {
      System.out.println("There is not containers for remove");
    }

    //Pull image
    String registryAuth = String.format("{\n"
        + "  \"serveraddress\": \"%s\""
        + "}", "registry.premiumitsolution.com");
    registryAuth = Base64.getEncoder().encodeToString(registryAuth.getBytes(StandardCharsets.UTF_8));
    Response<Void> createImageResponse = portainerDockerApi.createImage(endPoint.getId(), String.format("%s:%s", imageName, imageTag), registryAuth, apiToken)
        .execute();
    if (createImageResponse.code() != 200) {
      throw new RuntimeException("Error while pull container:" + createImageResponse.message());
    }

    // Create container
//    ContainerConfig containerConfig = new ContainerConfig();
//    containerConfig.image(String.format("%s:%s", imageName, imageTag));
//    Call<ContainerCreateResponse> callDeleteContainer = portainerDockerApi.createContainer(endPoint.getId(), containerConfig, containerName, apiToken);
//    Response<ContainerCreateResponse> dockerResponse = callDeleteContainer.execute();
//    if (dockerResponse.code() == 201) {
//      ContainerCreateResponse createResponse = dockerResponse.body();
//      StringJoiner sb = new StringJoiner("\n");
//      if (createResponse.getWarnings() != null) {
//        createResponse.getWarnings().forEach(sb::add);
//      }
//      System.out.printf("Created new container with id='%s', warnings='%s'%n", createResponse.getId(), sb);
//    } else {
//      throw new RuntimeException("Error while create container:" + dockerResponse.message());
//    }

  }

}
