package tpanda.maven.plugins.gcc.win;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import tpanda.maven.plugins.gcc.AbstractPackageLinkMojo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * オブジェクトファイルをリンクして、dllファイルを生成するプラグイン。
 * アーティファクトに、ヘッダファイルをzipでまとめたものと、インポートライブラリ(*.dll.a)をアタッチします。
 * <p>アタッチされる成果物のclassifier</p>
 * <dl>
 *     <dt>ヘッダファイル(*.zip)</dt><dd>header</dd>
 *     <dt>インポートライブラリ(*.dll.a)</dt><dd>imp-lib</dd>
 * </dl>
 */
@Mojo(name = WindowsConstructs.GOAL_LINK_DLL, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PackageLinkDLLMojo extends AbstractPackageLinkMojo implements WindowsConstructs {
    /**
     * アーティファクトにアタッチするヘッダファイルの格納ディレクトリを指定します。
     */
    @Parameter(defaultValue = "${project.basedir}/includes/main")
    private File includeDirectory;

    /**
     * リンカに渡す引数を指定します。
     */
    @Parameter(defaultValue = "-shared -static -Wl,--add-stdcall-alias,-s")
    private String ldflags;

    private Path importLibPath() {
        return directory.toPath().resolve("lib%s.%s".formatted(getArtifactFileBaseName(), "dll.a"));
    }

    private Path headersZipPath() {
        return directory.toPath().resolve("%s.%s".formatted(getArtifactFileBaseName(), "zip"));
    }

    @Override
    protected String getName() {
        return GOAL_LINK_DLL;
    }

    @Override
    protected String getLinkerOptions() {
        return ldflags + " -Wl,--out-implib," + importLibPath();
    }

    @Override
    protected String getExtension() {
        return "dll";
    }

    @Override
    protected boolean isImportLib(Artifact artifact) {
        return "dll.a".equals(artifact.getType());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // デフォルトのリンク処理を実行して、メインのアーティファクトを作成する
            super.execute();

            // インポートライブラリをアタッチする(リンカへの引数にインポートライブラリの作成を渡すため、スーパークラスの処理ですでに生成済みとなるはず)
            projectHelper.attachArtifact(project, "dll.a", CLASSIFIER_IMPLIB, importLibPath().toFile());

            // ヘッダファイルをアタッチする
            try (
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(headersZipPath())));
                Stream<Path> headers = Files.walk(includeDirectory.toPath()).filter(Files::isRegularFile);
            ) {
                Iterator<Path> iter = headers.iterator();
                while (iter.hasNext()) {
                    final Path it = iter.next();
                    ZipEntry entry = new ZipEntry(includeDirectory.toPath().relativize(it).toString().replace(File.separatorChar, '/'));
                    out.putNextEntry(entry);
                    Files.copy(it, out);
                    out.closeEntry();
                }
            }
            projectHelper.attachArtifact(project, "zip", CLASSIFIER_HEADERS, headersZipPath().toFile());

        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }
}
