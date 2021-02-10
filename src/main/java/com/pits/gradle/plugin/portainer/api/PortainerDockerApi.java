package com.pits.gradle.plugin.portainer.api;

import com.pits.gradle.plugin.portainer.dto.DockerResponse;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
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
  Call<DockerResponse> removeContainer(@Path("endpointId") Integer endPointId,
      @Path("containerId") String containerId,
      @Query(value = "v") Boolean removeAnonymousVolumes,
      @Query(value = "force") Boolean forceKill,
      @Query(value = "link") Boolean removeLink,
      @Header("Authorization") String authHeader);

}
