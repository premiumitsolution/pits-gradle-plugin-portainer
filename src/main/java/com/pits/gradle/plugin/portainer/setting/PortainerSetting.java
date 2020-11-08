package com.pits.gradle.plugin.portainer.setting;

import org.gradle.api.provider.Property;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
 public abstract class PortainerSetting {

  abstract public Property<String> getPortainerUrl();

  abstract public Property<String> getPortainerLogin();

  abstract public Property<String> getPortainerPassword();

  abstract public Property<String> getDockerImageName();

}
