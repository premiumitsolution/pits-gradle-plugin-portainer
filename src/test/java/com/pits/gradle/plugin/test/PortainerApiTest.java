package com.pits.gradle.plugin.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pits.gradle.plugin.data.ApiClient;
import com.pits.gradle.plugin.data.ApiException;
import com.pits.gradle.plugin.data.controller.AuthApi;
import com.pits.gradle.plugin.data.controller.ContainerApi;
import com.pits.gradle.plugin.data.controller.EndpointsApi;
import com.pits.gradle.plugin.data.dto.AuthenticateUserRequest;
import com.pits.gradle.plugin.data.dto.AuthenticateUserResponse;
import com.pits.gradle.plugin.data.dto.ContainerSummary;
import com.pits.gradle.plugin.data.dto.EndpointSubset;
import com.pits.gradle.plugin.portainer.api.PortainerDockerApi;
import com.pits.gradle.plugin.portainer.dto.DockerResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    String imageName = "erp/pits-erp-project";

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
    List<ContainerSummary> containerSummaryList = containerApi.endpointContainerList(endPoint.getId(), true, null, null, null);

    PortainerDockerApi portainerDockerApi = initDockerApi();
    if (containerSummaryList != null) {
      List<ContainerSummary> foundedContainers = containerSummaryList.stream()
          .filter(containerSummary -> containerSummary.getImage() != null && containerSummary.getImage().contains(imageName)).collect(Collectors.toList());

      for (ContainerSummary containerSummary : foundedContainers) {
        System.out.printf("Remove container with id='%s', name='%s' and image='%s'%n", containerSummary.getId(), containerSummary.getNames(),
            containerSummary.getImage());
        Call<DockerResponse> callDeleteContainer = portainerDockerApi.removeContainer(endPoint.getId(), containerSummary.getId(), true, true, false, apiToken);
        Response<DockerResponse> dockerResponse = callDeleteContainer.execute();
        if (dockerResponse.code() != 204) {
          if (dockerResponse.body() != null) {
            throw new RuntimeException(dockerResponse.body().getMessage());
          } else {
            throw new RuntimeException("Error while delete container:" + dockerResponse.message());
          }
        }
      }
    }
  }

}
