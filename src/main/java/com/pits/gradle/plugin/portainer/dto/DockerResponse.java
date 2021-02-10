package com.pits.gradle.plugin.portainer.dto;

import lombok.Data;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
@Data
public class DockerResponse {

  /**
   * Message from container.
   */
  private String message;

}
