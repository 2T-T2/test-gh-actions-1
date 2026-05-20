package tpanda.maven.plugins.gcc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import tpanda.maven.plugins.gcc.win.WindowsConstructs;

/**
 * C++ ソースファイルをコンパイルしてオブジェクトファイルを生成するプラグイン
 */
@Mojo(name = Constructs.GOAL_COMPILE_CXX, defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileCXXMojo extends AbstractCompileMojo implements Constructs {
    /**
     * コンパイル対象とするソースファイルを検索するときのパターンを指定します。
     */
    @Parameter(defaultValue = "*.{cpp,cxx,cc}")
    private String sourcePattern;
    /**
     * 使用するコンパイラを指定します。
     */
    @Parameter(defaultValue = "g++")
    private String compiler;
    /**
     * コンパイラに渡す引数を指定します。
     */
    @Parameter(defaultValue = "-Wall -Wextra -Werror -g")
    private String cxxflags;

    @Override
    protected String getName() {
        return GOAL_COMPILE_CXX;
    }

    @Override
    protected String getCompiler() {
        return compiler;
    }

    @Override
    protected String getCompileFlags() {
        return cxxflags;
    }

    @Override
    protected String getSourcePattern() {
        return sourcePattern;
    }
}
