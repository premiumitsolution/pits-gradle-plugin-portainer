package com.pits.gradle.plugin.portainer.setting;

import org.gradle.api.provider.Property;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public abstract class PortainerSetting {

  abstract public Property<String> getPortainerApiUrl();

  abstract public Property<String> getPortainerLogin();

  abstract public Property<String> getPortainerPassword();

  abstract public Property<String> getPortainerEndPointName();

  abstract public Property<String> getDockerImageName();

  abstract public Property<String> getDockerImageTag();

  abstract public Property<String> getContainerName();
}
