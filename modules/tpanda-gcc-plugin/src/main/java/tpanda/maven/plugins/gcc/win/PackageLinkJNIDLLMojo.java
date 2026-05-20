package tpanda.maven.plugins.gcc.win;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import tpanda.maven.plugins.gcc.AbstractPackageLinkMojo;

import java.util.Optional;

/**
 * オブジェクトファイルをリンクして、jni用のdllファイルを生成するプラグイン。
 * {@link PackageLinkDLLMojo} との大きな違いは、インポートライブラリやヘッダファイルをアーティファクトにアタッチしない点です。
 */
@Mojo(name = WindowsConstructs.GOAL_LINK_JNI_DLL, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PackageLinkJNIDLLMojo extends AbstractPackageLinkMojo implements WindowsConstructs {
    /**
     * リンカに渡す引数を指定します。
     */
    @Parameter(defaultValue = "-shared -static -Wl,--add-stdcall-alias,-s")
    private String ldflags;

    @Override
    protected String getName() {
        return GOAL_LINK_JNI_DLL;
    }

    @Override
    protected String getLinkerOptions() {
        return ldflags;
    }

    @Override
    protected String getExtension() {
        return "dll";
    }

    @Override
    protected boolean isImportLib(Artifact artifact) {
        return "dll.a".equals(artifact.getType());
    }
}
