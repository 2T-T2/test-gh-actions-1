package tpanda.maven.plugins.gcc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import tpanda.maven.plugins.gcc.win.WindowsConstructs;

/**
 * C ソースファイルをコンパイルしてオブジェクトファイルを生成するプラグイン
 */
@Mojo(name = Constructs.GOAL_COMPILE_CC, defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileCCMojo extends AbstractCompileMojo implements Constructs {
    /**
     * コンパイル対象とするソースファイルを検索するときのパターンを指定します。
     */
    @Parameter(defaultValue = "*.c")
    private String sourcePattern;
    /**
     * 使用するコンパイラを指定します。
     */
    @Parameter(defaultValue = "gcc")
    private String compiler;
    /**
     * コンパイラに渡す引数を指定します。
     */
    @Parameter(defaultValue = "-Wall -Wextra -Werror -g")
    private String ccflags;

    @Override
    protected String getName() {
        return GOAL_COMPILE_CC;
    }

    @Override
    protected String getCompiler() {
        return compiler;
    }

    @Override
    protected String getCompileFlags() {
        return ccflags;
    }

    @Override
    protected String getSourcePattern() {
        return sourcePattern;
    }
}
