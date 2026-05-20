// compile フェーズの出力ファイルのテスト
assert new File( basedir, "target/objects/main.o" ).isFile()

// package フェーズの出力ファイルのテスト
def artifact = new File( basedir, "target/build-exe-it.exe" )
assert artifact.isFile()

// 作成された実行ファイルが実行できるかをテスト
def pb = new ProcessBuilder(artifact.toString())
pb.redirectErrorStream(true)

def process = pb.start()
process.in.eachLine { line ->
    println "出力: ${line}"
}
process.waitFor()

assert process.exitValue() == 0
