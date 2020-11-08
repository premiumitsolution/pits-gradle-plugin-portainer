package com.pits.gradle.plugin.portainer.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;


/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public abstract class DeployImageToPortainerTask extends DefaultTask {

  @Input
  abstract public Property<String> getPortainerUrl();

  @Input
  abstract public Property<String> getPortainerLogin();

  @Input
  abstract public Property<String> getPortainerPassword();

  @Input
  abstract public Property<String> getDockerImageName();

  @TaskAction
  public void deployImage() {
    System.out.printf("Deploy image '%s' to portainer '%s'%n", getPortainerUrl().get(), getDockerImageName().get());
  }

}
