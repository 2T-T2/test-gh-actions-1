def prj_dll = new File(basedir, "dll")
def prj_exe = new File(basedir, "exe")
def prj_run = new File(basedir, "run")

PROJECT_DLL : {
    // compile フェーズの出力ファイルのテスト
    assert new File( prj_dll, "target/objects/mymath.o" ).isFile()

    // package フェーズの出力ファイルのテスト
    def artifactBaseName = "dependency-dll-it"
    assert new File( prj_dll, "target/%s.dll".formatted(artifactBaseName) ).isFile()
    assert new File( prj_dll, "target/%s.zip".formatted(artifactBaseName) ).isFile()
    assert new File( prj_dll, "target/lib%s.dll.a".formatted(artifactBaseName) ).isFile()
}

PROJECT_EXE : {
    // compile フェーズの出力ファイルのテスト
    assert new File( prj_exe, "target/objects/main.o" ).isFile()

    // package フェーズの出力ファイルのテスト
    assert new File( prj_exe, "target/dependency-exe-it.exe" ).isFile()
}

PROJECT_RUN : {
    def target     = new File( prj_run, "dependency-exe-it.exe" )
    def dependency = new File( prj_run, "dependency-dll-it.dll" )

    // dependency-copy で 成果物がとってこれているかテスト
    assert target.isFile()
    assert dependency.isFile()

    // 作成された実行ファイルが実行できるかをテスト
    def pb = new ProcessBuilder(target.toString())
    pb.redirectErrorStream(true)

    def process = pb.start()
    process.in.eachLine { line ->
        println "出力: ${line}"
    }
    process.waitFor()

    assert process.exitValue() == 0
}
