package tpanda.maven.plugins;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class MyMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir" ,required = true)
    private File outputDirectory;

    public void execute() throws MojoExecutionException {
System.out.println("  outputDirectory: " + outputDirectory);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException("フォルダの作成に失敗しました。");
            }
        }
        File touch = new File(outputDirectory, "touch.txt");

        try (FileWriter w = new FileWriter(touch)) {
            w.write("touch.txt");

        } catch (IOException e) {
            throw new MojoExecutionException("Error creating file " + touch, e);
        }
    }
}
