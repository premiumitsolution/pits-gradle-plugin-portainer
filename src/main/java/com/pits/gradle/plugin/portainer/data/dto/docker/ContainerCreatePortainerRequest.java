package com.pits.gradle.plugin.portainer.data.dto.docker;

import com.google.gson.annotations.SerializedName;
import com.pits.gradle.plugin.data.docker.dto.ContainerConfig;
import com.pits.gradle.plugin.data.docker.dto.HostConfig;
import com.pits.gradle.plugin.data.docker.dto.NetworkingConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerCreatePortainerRequest extends ContainerConfig {

  @SerializedName("HostConfig")
  private HostConfig hostConfig;

  @SerializedName("NetworkingConfig")
  private NetworkingConfig networkingConfig;
}
