package com.pits.gradle.plugin.portainer.setting;

import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public abstract class PortainerSetting {

  @Getter
  private final ContainerAccessSetting containerAccess;

  public PortainerSetting(ObjectFactory objects) {
    this.containerAccess = objects.newInstance(ContainerAccessSetting.class);
  }

  public void containerAccess(Action<? super ContainerAccessSetting> action) {
    action.execute(containerAccess);
  }

  abstract public Property<String> getPortainerApiUrl();

  abstract public Property<String> getPortainerLogin();

  abstract public Property<String> getPortainerPassword();

  abstract public Property<String> getPortainerEndPointName();

  abstract public Property<String> getDockerImageName();

  abstract public Property<String> getDockerImageTag();

  abstract public Property<String> getContainerName();

  abstract public Property<String> getRegistryUrl();

  abstract public Property<String> getPublishedPorts();

  abstract public Property<Boolean> getRemoveOldImages();

  abstract public Property<String> getRestartPolicy();

  abstract public MapProperty<String, String> getVolumes();

}
