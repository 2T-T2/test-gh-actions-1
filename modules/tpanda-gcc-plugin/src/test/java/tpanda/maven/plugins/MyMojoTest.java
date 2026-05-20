package tpanda.maven.plugins;


import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@MojoTest
public class MyMojoTest {
    @Inject
    private MavenSession session;

    @Inject
    private MavenProject project;

    @Test
    @Basedir("target/test-classes/touch-test/")
    @InjectMojo( goal = "touch", pom = "pom1.xml")
    public void testSetOutputDirectoryFromPluginConfiguration(MyMojo myMojo) throws Exception {
        System.out.println("test pom: " + project.getFile());
        Path expectOutputPath = Path.of("target/test-classes/touch-test/target/test-harness/touch-test/touch.txt");

        if (Files.exists(expectOutputPath))
            Files.delete(expectOutputPath);

        assertNotNull( myMojo );
        myMojo.execute();

        assertTrue(Files.exists(expectOutputPath));
    }

    @Test
    @Basedir("target/test-classes/touch-test/")
    @InjectMojo( goal = "touch", pom = "pom2.xml")
    public void testSetOutputDirectoryFromBuildDirectory(MyMojo myMojo) throws Exception {
        System.out.println("test pom: " + project.getFile());
        Path expectOutputPath = Path.of(project.getBuild().getDirectory(), "touch.txt");

        if (Files.exists(expectOutputPath))
            Files.delete(expectOutputPath);

        assertNotNull(myMojo);
        myMojo.execute();

        assertTrue(Files.exists(expectOutputPath));
    }
}

