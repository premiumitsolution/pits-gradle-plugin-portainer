package com.pits.gradle.plugin.portainer;

import com.pits.gradle.plugin.portainer.setting.PortainerSetting;
import com.pits.gradle.plugin.portainer.task.DeployImageToPortainerTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public class PortainerPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    PortainerSetting portainerSetting = project.getExtensions().create("portainerSetting", PortainerSetting.class);
    project.getTasks().register("deployImageToPortainer", DeployImageToPortainerTask.class, task -> {
      task.getPortainerLogin().set(portainerSetting.getPortainerLogin());
      task.getPortainerPassword().set(portainerSetting.getPortainerPassword());
      task.getPortainerApiUrl().set(portainerSetting.getPortainerApiUrl());
      task.getDockerImageName().set(portainerSetting.getDockerImageName());
      task.getDockerImageTag().set(portainerSetting.getDockerImageTag());
      task.getContainerName().set(portainerSetting.getContainerName());
      task.getRegistryUrl().set(portainerSetting.getRegistryUrl());
      task.getPortainerEndPointName().set(portainerSetting.getPortainerEndPointName());
      task.getPublishedPorts().set(portainerSetting.getPublishedPorts());
      task.getRemoveOldImages().set(portainerSetting.getRemoveOldImages());
      task.getRestartPolicy().set(portainerSetting.getRestartPolicy());
      task.getContainerAccess().set(portainerSetting.getContainerAccess());
      task.getVolumes().set(portainerSetting.getVolumes());
    });
  }
}
