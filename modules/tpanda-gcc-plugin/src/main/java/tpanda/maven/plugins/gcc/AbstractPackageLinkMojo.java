package tpanda.maven.plugins.gcc;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import tpanda.maven.plugins.gcc.command.Command;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GCC で オブジェクトファイルをリンク用の基底クラス
 * リンカに渡す引数はそれぞれの実装クラスで設定します
 * (デフォルト値をそれぞれの用途で設定するため)
 */
public abstract class AbstractPackageLinkMojo extends AbstractMojo implements Constructs {
    /**
     * このプラグインが実行されている現在のMavenプロジェクトインスタンスです。
     * 内部処理でプロジェクトの情報（ディレクトリ構造や依存関係など）を参照するために使用されます。
     * ユーザーがこのパラメータを直接設定することはありません。
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * このプロジェクトの依存関係です。
     * 内部処理でプロジェクトの情報（ディレクトリ構造や依存関係など）を参照するために使用されます。
     * ユーザーがこのパラメータを直接設定することはありません。
     */
    @Parameter(defaultValue = "${project.artifacts}", readonly = true)
    protected Set<Artifact> artifacts;
    /**
     * このプロジェクトのアーティファクトのインスタンスです。
     * 内部処理でプロジェクトの情報（ディレクトリ構造や依存関係など）を参照するために使用されます。
     * ユーザーがこのパラメータを直接設定することはありません。
     */
    @Parameter(defaultValue = "${project.artifact}", readonly = true)
    protected Artifact artifact;

    /**
     * このプラグインの成果物を配置するディレクトリとして使用します。
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected File directory;
    /**
     * リンクするオブジェクトファイル(*.o)を検索するディレクトリとして使用します。
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File outputDirectory;

    /**
     * 生成したファイルをメインのアーティファクトではなく、サブのアーティファクトとしてアタッチする際の名前を指定します。<br>
     * 生成したファイルをメインのアーティファクトにする場合は未設定でOKです。
     */
    @Parameter()
    protected String classifier;

    /**
     * コンパイラに渡す引数を指定します。
     */
    @Parameter(defaultValue = "-Wall -Wextra -Werror -g")
    protected String cxxflags;
    /**
     * プリプロセッサに渡す引数を指定します。
     */
    @Parameter()
    protected String cppflags;
    /**
     * 対象とするアーキテクチャを指定します
     */
    @Parameter()
    protected String targetArch;

    /**
     * 冗長な出力を行います。
     */
    @Parameter(defaultValue = "false")
    protected boolean verbose;

    /**
     * 実装Mojoの名前を返却します
     * @return 実装Mojoの名前
     */
    protected abstract String getName();

    /**
     * リンカに渡すオプションを返却します
     * @return リンカに渡すオプション
     */
    protected abstract String getLinkerOptions();

    /**
     * 成果物をアーティファクトにアタッチする際の名前を取得します。
     * {@link Optional#empty()} の場合は、メインのアーティファクトとして登録します。
     * デフォルト実装では パラメーター {@link #classifier} を {@link Optional#ofNullable(Object)} にした値を返却します。
     * mojo によって classifier を固定化したい場合は、オーバーライドしてください。
     * @return アーティファクトにアタッチする際の名前
     */
    protected Optional<String> getClassifier() {
        return Optional.ofNullable(classifier);
    }

    /**
     * 成果物の拡張子を取得します。
     * packaging に紐づいた拡張子を返却させる場合は以下のような値を返却することができます。
     * <code>
     *     artifactHandlerManager.getArtifactHandler(packaging).getExtension()
     * </code>
     * @return 成果物の拡張子を取得します。
     */
    protected abstract String getExtension();

    /**
     * 指定された依存関係が、リンク時に参照されるインポートライブラリかどうかを判定します。
     * <code>
     *     return "dll.a".equals(artifact.getType()); // windows 向けの実装例
     * </code>
     * @param artifact インポートライブラリかどうかを判定する依存関係
     * @return 指定された依存関係が、リンク時に参照されるインポートライブラリかどうか
     */
    protected abstract boolean isImportLib(Artifact artifact);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // 入力チェック(エラー)
            if (outputDirectory == null)
                throw new MojoFailureException("outputDirectory is not set");

            if (artifacts == null)
                artifacts = Set.of();

            if (!outputDirectory.exists()) {
                getLog().warn("not found %s".formatted(outputDirectory.toString()));
                return;
            }

            Path objDir = outputDirectory.toPath();
            Path outDir = directory.toPath();
            Path dependencyDir = outDir.resolve(DEPENDENCY_IMPLIB_DIR_RELATIVE).normalize();
            Path artifactFile = directory.toPath().resolve("%s.%s".formatted(getArtifactFileBaseName(), getExtension()));
            String ldflags = getLinkerOptions();
            Map<Artifact, String> impLibNames = artifacts.stream()  // key: 依存関係アーティファクト, value: リンク時に使用する名前
                .filter(this::isImportLib)  // 依存関係からインポートライブラリを抽出
                .collect(Collectors.toMap(it->it, Artifact::getArtifactId)) // リンク時に使用する名前を取得する。
            ;

            // 出力先ディレクトリの作成
            if (!Files.exists(outDir))
                Files.createDirectories(outDir);
            if (!Files.exists(dependencyDir))
                Files.createDirectories(dependencyDir);

            // 依存関係からインポートライブラリをコピー
            if (verbose) getLog().debug("依存関係からインポートライブラリをコピー");
            for (Map.Entry<Artifact, String> entry : impLibNames.entrySet()) {
                final Artifact dependency = entry.getKey();
                final String libName = entry.getValue();

                Path from = dependency.getFile().toPath();
                Path to =  dependencyDir.resolve("lib%s.dll.a".formatted(libName));

                if (verbose) getLog().debug("  [%s->%s]".formatted(from, to));

                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            }

            // リンク実行コマンドの構築
            Command.Builder builder = new Command.Builder("g++")
                .addArgs(cxxflags)
                .addArgs(cppflags)
                .addArgs(targetArch)
            ;
            // オブジェクトファイルリンク
            try (Stream<Path> objFiles = Files.walk(objDir).filter(Files::isRegularFile).filter(this::isObjectFile)) {
                objFiles.map(Object::toString).forEach(builder::addArgs);
            }
            // リンカに渡す引数
            if (ldflags != null && !ldflags.isEmpty()) {
                builder.addArgs(ldflags);
            }
            builder.addArgs("-static")      // 静的リンク可能なものは静的リンク
                .addArgs("-Wl,-Bdynamic")
                .addArgs("-L")
                .addArgs(dependencyDir.toString())
            ;
            impLibNames.values().stream()   // リンク時に使用するライブラリ名を取得
                .map("-l%s"::formatted)     // リンカに渡す引数形式に変換
                .forEach(builder::addArgs)  // リンカに渡す引数に追加
            ;
            builder.addArgs("-Wl,-Bstatic");// 静的リンク可能なものは静的リンク

            // 出力ファイル設定
            builder.addArgs("-o")
                .addArgs(artifactFile.toString());

            if (artifactFile.getParent() != null && !Files.exists(artifactFile.getParent())) {
                Files.createDirectories(artifactFile.getParent());
            }

            // リンク実行
            Command command = builder.build();
            int returnCode = command.execute(GCC_CHARSET, getLog());
            if (returnCode != 0) {
                throw new MojoFailureException("link error (rc=%d)".formatted(returnCode));
            }

            getLog().info("");  // ログに空行

            // 正常にビルドが出来たら、アーティファクトに登録/アタッチする
            getClassifier().ifPresentOrElse(
                (String classifier) -> {
                    getLog().info("attach %s as %s".formatted(artifactFile, classifier));
                    projectHelper.attachArtifact(project, getExtension(), classifier, artifactFile.toFile());
                },
                () -> {
                    getLog().info("register artifact %s".formatted(artifactFile));
                    artifact.setFile(artifactFile.toFile());
                }
            );

        } catch (MojoFailureException e) {
            throw e;

        } catch (Throwable e) {
            throw new MojoExecutionException(String.format("An error occurred while executing '%s'", getName()), e);
        }
    }

    protected final String getArtifactFileBaseName() {
        return artifact.getArtifactId();
    }

    private boolean isObjectFile(Path path) {
        return FileSystems.getDefault().getPathMatcher("glob:*.o").matches(path.getFileName());
    }

}
