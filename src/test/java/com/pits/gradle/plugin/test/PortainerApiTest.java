package com.pits.gradle.plugin.test;

import com.pits.gradle.plugin.data.ApiClient;
import com.pits.gradle.plugin.data.ApiException;
import com.pits.gradle.plugin.data.controller.AuthApi;
import com.pits.gradle.plugin.data.controller.ContainerApi;
import com.pits.gradle.plugin.data.dto.AuthenticateUserRequest;
import com.pits.gradle.plugin.data.dto.AuthenticateUserResponse;
import com.pits.gradle.plugin.data.dto.ContainerSummary;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.gradle.internal.impldep.org.apache.http.util.Asserts;
import org.junit.jupiter.api.Test;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public class PortainerApiTest {

  @Test
  public void testAuth() throws ApiException {
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
    System.out.println(response.getJwt());

    apiClient.addDefaultHeader("Authorization", "Bearer "+response.getJwt());

    ContainerApi containerApi = new ContainerApi(apiClient);
    List<ContainerSummary> containerSummaryList = containerApi.endpointContainerList(8, true, null, null, null);
    if (containerSummaryList!=null){
      containerSummaryList.forEach(containerSummary -> {
        System.out.println(containerSummary.toString());
      });
      System.out.println("Container size:" + containerSummaryList.size());
    }

    //endpointsApi.


  }

}
