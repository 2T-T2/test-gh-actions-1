// compile フェーズの出力ファイルのテスト
assert new File( basedir, "target/objects/mymath.o" ).isFile()

// package フェーズの出力ファイルのテスト
def artifactBaseName = "build-dll-it"
assert new File( basedir, "target/%s.dll".formatted(artifactBaseName) ).isFile()
assert new File( basedir, "target/%s.zip".formatted(artifactBaseName) ).isFile()
assert new File( basedir, "target/lib%s.dll.a".formatted(artifactBaseName) ).isFile()
