package tpanda.maven.plugins.gcc;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import tpanda.maven.plugins.gcc.command.Command;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * GCC コンパイルの Mojo の基底クラス
 * C/C++ のコンパイル処理の共通部分を抽出しています
 */
public abstract class AbstractCompileMojo extends AbstractMojo implements Constructs {
    @Parameter(defaultValue = "${project.artifacts}", readonly = true, required = true)
    private Set<Artifact> artifacts;

    // I/O ディレクトリの設定
    /**
     * ビルドで使用するディレクトリのを指定します。コンパイル時に参照する依存関係の一時コピー等のファイルもこのディレクトリのサブディレクトリに配置されます。
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File directory;
    /**
     * コンパイル対象とするソースファイルの格納ディレクトリを指定します。
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true)
    private File sourceDirectory;
    /**
     * コンパイルの出力ファイル(*.o)の出力先ディレクトリ
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputDirectory;

    // インクルードディレクトリの設定
    /**
     * インクルードディレクトリを指定します
     */
    @Parameter(defaultValue = "${project.basedir}/includes/main")
    private File includeDirectory;
    /**
     * 追加のインクルードディレクトリを指定します。JNI開発などで自動生成されるヘッダファイルの出力ディレクトリ等を指定する場合などに使用します。
     */
    @Parameter()
    private Set<File> additionalIncludeDirectories;
    /**
     * JNI開発用のヘッダファイルが格納されているディレクトリをインクルードディレクトリに追加します。
     * 追加されるディレクトリは、システムプロパティ["java.home"]/includeとその配下のサブディレクトリです。
     * @see System#getProperty(String)
     */
    @Parameter(defaultValue = "false")
    private boolean includeJNI;

    // 依存関係周りの設定
    /**
     * 依存関係のzipファイルからコンパイル時に参照するヘッダファイルを抽出するときのパターン。
     * 依存関係に含まれるzipファイル全てを対象に中身を走査して、このパターンに一致するヘッダファイルをコンパイル時に参照するヘッダファイルに追加します。
     */
    @Parameter(defaultValue = "*.{hpp,h}")
    private String headerPattern;

    // プリプロセッサ/コンパイラへ渡す設定
    /**
     * プリプロセッサに渡す引数を指定します
     */
    @Parameter()
    private String cppflags;
    /**
     * 対象とするアーキテクチャを指定します
     */
    @Parameter()
    private String targetArch;

    // 動作全体に影響のある設定
    /**
     * 並列コンパイルのjob数を指定します。
     */
    @Parameter(defaultValue = "1")
    private int job;
    /**
     * 冗長な出力を行います。
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    /**
     * 実装Mojoの名前を返却します
     * @return 実装Mojoの名前
     */
    protected abstract String getName();

    /**
     * C/C++ のコンパイルで使用するコンパイラを返却します
     * @return C/C++ のコンパイルで使用するコンパイラ
     */
    protected abstract String getCompiler();

    /**
     * コンパイルオプションを返却します
     * @return コンパイルオプション
     */
    protected abstract String getCompileFlags();

    /**
     * コンパイル対象とするソースファイルのパターンを返却します
     * @return コンパイル対象とするソースファイルのパターン
     */
    protected abstract String getSourcePattern();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // 入力チェック(エラー)
            if (sourceDirectory == null)
                throw new MojoFailureException("sourceDirectory is not set");
            if (outputDirectory == null)
                throw new MojoFailureException("outputDirectory is not set");
            if (includeDirectory == null)
                throw new MojoFailureException("includeDirectory is not set");
            if (!sourceDirectory.exists())
                throw new MojoFailureException(String.format("source directory not found: %s", sourceDirectory.getAbsolutePath()));

            // 入力チェック(規定値設定)
            if (job <= 0) {
                getLog().warn("invalid job value %d, fallback to 1".formatted(job));
                job = 1;
            }
            int maxJob = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
            if (job > maxJob) {
                getLog().warn("job %d is too large, cap to %d".formatted(job, maxJob));
                job = maxJob;
            }
            if (artifacts == null)
                artifacts = Set.of();
            if (additionalIncludeDirectories == null)
                additionalIncludeDirectories = Set.of();
            if (includeJNI)
                additionalIncludeDirectories.addAll(getJNIHeaderDirectories());

            Path srcDir = sourceDirectory.toPath();
            Path outDir = directory.toPath();
            Path objDir = outputDirectory.toPath();
            Path incDir = includeDirectory.toPath();
            Path dependencyHeaderDir = outDir.resolve(DEPENDENCY_HEADER_DIR_RELATIVE).normalize();
            Set<Path> additionalIncludeDir = additionalIncludeDirectories.stream().map(File::toPath).collect(Collectors.toSet());

            // 出力先ディレクトリの作成
            if (!Files.exists(outDir))
                Files.createDirectories(outDir);
            if (!Files.exists(dependencyHeaderDir))
                Files.createDirectories(dependencyHeaderDir);

            // 依存関係に含まれるヘッダファイルをコピーする
            artifacts.stream().filter(this::isZip).map(Artifact::getFile).filter(Objects::nonNull).map(File::toPath).forEach(path -> {
                if (verbose) getLog().debug("依存関係に含まれるヘッダファイルをコピーする[%s]".formatted(path));

                try (ZipInputStream in = new ZipInputStream(Files.newInputStream(path))) {
                    ZipEntry entry;
                    while (Objects.nonNull((entry = in.getNextEntry()))) {
                        if (!isHeaderFile(entry)) continue;
                        Path dst = dependencyHeaderDir.resolve(Path.of(entry.getName())).normalize();

                        if (!dst.startsWith(dependencyHeaderDir))
                            throw new IOException("Zip entry escapes target dir %s".formatted(dst));

                        Files.createDirectories(dst.getParent());
                        try (OutputStream out = Files.newOutputStream(dst)) {
                            in.transferTo(out);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            // コンパイルタスクの生成
            List<Callable<Integer>> tasks = new ArrayList<>();
            try (Stream<Path> sources = Files.walk(srcDir)) {
                sources.filter(Files::isRegularFile).filter(this::isSource).map(path -> Map.entry(path, src2out(path, srcDir, objDir))).forEach(entry -> {
                    final Path source = entry.getKey();
                    final Path object = entry.getValue();

                    tasks.add(() -> {
                        if (Files.exists(object)) {
                            if (Files.getLastModifiedTime(source).compareTo(Files.getLastModifiedTime(object)) < 0)
                                return 0;
                        }

                        Command command = new Command.Builder(getCompiler())
                            .addArgs(getCompileFlags())
                            .addArgs(cppflags)
                            .addArgs(targetArch)
                            .addArgs("-c")
                            .addArgs("-o")
                            .addArgs(object.toString())
                            .addArgs("-I" + srcDir)
                            .addArgs("-I" + incDir)
                            .addArgs("-I" + dependencyHeaderDir)
                            .addArgs(additionalIncludeDir.stream().map(Path::toString).map(it->"-I"+it).toList())
                            .addArgs(source.toString())
                        .build();

                        if (object.getParent() != null && !Files.exists(object.getParent())) {
                            Files.createDirectories(object.getParent());
                        }

                        return command.execute(GCC_CHARSET, getLog());
                    });
                });
            }

            // コンパイルタスクを非同期実行開始
            try (ExecutorService executor = Executors.newFixedThreadPool(job)) {
                Throwable occurredException = null;
                List<Future<Integer>> compileFutures = executor.invokeAll(tasks);
                try {
                    for (Future<Integer> future : compileFutures) {
                        int returnCode = future.get();
                        if (returnCode != 0) {
                            throw new MojoFailureException("compile error (rc=%d)".formatted(returnCode));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    occurredException = e;

                } catch (ExecutionException e) {
                    occurredException = e.getCause();

                }  catch (Exception e) {
                    occurredException = e;

                } finally {
                    if (occurredException != null) {
                        for (Future<Integer> future : compileFutures) future.cancel(true);
                        executor.shutdownNow();
                        throw occurredException;
                    }

                    executor.shutdown();
                }
            }

        } catch (MojoFailureException e) {
            throw e;

        } catch (Throwable e) {
            throw new MojoExecutionException(String.format("An error occurred while executing '%s'", getName()), e);
        }
    }

    private Set<File> getJNIHeaderDirectories() throws IOException {
        Path home = new File(System.getProperty("java.home")).toPath();
        try (Stream<Path> includes = Files.walk(home.resolve("include"))) {
            return includes
                .filter(Files::isDirectory)
                .map(Path::toFile)
            .collect(Collectors.toSet());
        }
    }

    private boolean isSource(Path path) {
        return path.getFileSystem().getPathMatcher("glob:"+getSourcePattern()).matches(path.getFileName());
    }

    private boolean isZip(Artifact artifact) {
        if (artifact.getType() == null)
            return false;
        return artifact.getType().equalsIgnoreCase("zip");
    }

    private boolean isHeaderFile(ZipEntry entry) {
        if (entry.isDirectory())
            return false;

        return FileSystems.getDefault().getPathMatcher("glob:"+headerPattern).matches(Path.of(entry.getName()));
    }

    private Path src2out(Path src, Path srcdir, Path outdir) {
        Path rel = srcdir.relativize(src);
        Path filename = replaceExtension(rel.getFileName(), ".o");
        Path parent = rel.getParent() == null ? Path.of("") : rel.getParent();
        return outdir.resolve(parent).resolve(filename);
    }

    private Path replaceExtension(Path p, String ext) {
        String s = p.getFileName().toString();
        int i = s.lastIndexOf('.');
        if (i == -1)
            return p;

        s = s.substring(0, i) + ext;

        return p.resolveSibling(Path.of(s));
    }
}
