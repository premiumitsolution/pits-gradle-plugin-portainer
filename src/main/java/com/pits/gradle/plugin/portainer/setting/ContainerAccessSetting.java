package com.pits.gradle.plugin.portainer.setting;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public abstract class ContainerAccessSetting {

  abstract public ListProperty<String> getTeams();

  abstract public Property<Boolean> getAdministratorsOnly();

  abstract public Property<Boolean> getPublicAccess();


}
