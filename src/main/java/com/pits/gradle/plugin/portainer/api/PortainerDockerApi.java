package com.pits.gradle.plugin.portainer.api;

import com.pits.gradle.plugin.data.docker.dto.ContainerCreateResponse;
import com.pits.gradle.plugin.portainer.data.dto.docker.ContainerCreatePortainerRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API for with portainer docker api.
 *
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public interface PortainerDockerApi {

  /**
   * Delete container by id.
   *
   * @param endPointId endpoint id
   * @param containerId container id
   * @param removeAnonymousVolumes Remove anonymous volumes associated with the container.
   * @param forceKill If the container is running, kill it before removing it.
   * @param removeLink Remove the specified link associated with the container.
   * @param authHeader auth header
   * @return DockerResponse if error is present
   * @see <a href="https://docs.docker.com/engine/api/v1.30/#operation/ContainerDelete">Docker ContainerDelete</a>
   */
  @DELETE("endpoints/{endpointId}/docker/containers/{containerId}")
  Call<Void> removeContainer(@Path("endpointId") Integer endPointId,
      @Path("containerId") String containerId,
      @Query(value = "v") Boolean removeAnonymousVolumes,
      @Query(value = "force") Boolean forceKill,
      @Query(value = "link") Boolean removeLink,
      @Header("Authorization") String authHeader);

  /**
   * Create an image by either pulling it from a registry or importing it.
   *
   * @param endPointId endpoint id
   * @param imageName image name for create from
   * @param registryAuth A base64url-encoded auth configuration.
   * @param authHeader auth header
   * @return DockerResponse if error is present
   * @see <a href="https://docs.docker.com/engine/api/v1.30/#operation/ContainerDelete">Docker ContainerDelete</a>
   */
  @POST("endpoints/{endpointId}/docker/images/create")
  Call<Void> createImage(@Path("endpointId") Integer endPointId, @Query(value = "fromImage") String imageName,
      @Header("X-Registry-Auth") String registryAuth,
      @Header("Authorization") String authHeader);

  /**
   * Delete container by id.
   *
   * @param endPointId endpoint id
   * @param name Assign the specified name to the container. Must match /?[a-zA-Z0-9_-]+.
   * @param containerConfig container create config
   * @param authHeader auth header
   * @return DockerResponse if error is present
   * @see <a href="https://docs.docker.com/engine/api/v1.30/#operation/ContainerDelete">Docker ContainerDelete</a>
   */
  @POST("endpoints/{endpointId}/docker/containers/create")
  Call<ContainerCreateResponse> createContainer(@Path("endpointId") Integer endPointId,
      @Body ContainerCreatePortainerRequest containerConfig,
      @Query(value = "name") String name,
      @Header("Authorization") String authHeader);

}
