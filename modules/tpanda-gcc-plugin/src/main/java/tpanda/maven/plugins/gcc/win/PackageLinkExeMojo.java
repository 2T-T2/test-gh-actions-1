package tpanda.maven.plugins.gcc.win;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import tpanda.maven.plugins.gcc.AbstractPackageLinkMojo;

import java.util.Optional;

/**
 * オブジェクトファイルをリンクして、exeファイルを生成するプラグイン。
 */
@Mojo(name = WindowsConstructs.GOAL_LINK_EXE, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PackageLinkExeMojo extends AbstractPackageLinkMojo implements WindowsConstructs {
    /**
     * リンカに渡す引数を指定します。
     */
    @Parameter(defaultValue = "-static -Wl,--add-stdcall-alias,-s")
    private String ldflags;

    @Override
    protected String getName() {
        return GOAL_LINK_EXE;
    }

    @Override
    protected String getLinkerOptions() {
        return ldflags;
    }

    @Override
    protected String getExtension() {
        return "exe";
    }

    @Override
    protected boolean isImportLib(Artifact artifact) {
        return "dll.a".equals(artifact.getType());
    }

}
