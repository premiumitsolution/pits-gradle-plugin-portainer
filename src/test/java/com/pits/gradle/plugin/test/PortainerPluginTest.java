package com.pits.gradle.plugin.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pits.gradle.plugin.portainer.task.DeployImageToPortainerTask;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import org.gradle.testfixtures.ProjectBuilder;

/**
 * @author m.gromov
 * @version 1.0
 * @since 1.0.0
 */
public class PortainerPluginTest {

  @Test
  public void deployImageToPortainerTaskTest() {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("com.pits.gradle.portainer");

    assertTrue(project.getTasks().getByName("deployImageToPortainer") instanceof DeployImageToPortainerTask);
  }

}
