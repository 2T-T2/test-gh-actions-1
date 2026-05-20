// compile フェーズの出力ファイルのテスト
assert new File( basedir, "target/objects/NativeMethods.o" ).isFile()

// package フェーズの出力ファイルのテスト
def artifactBaseName = "jni-it"
assert new File( basedir, "target/%s.dll".formatted(artifactBaseName) ).isFile()

// install フェーズでアタッチされてローカルリポジトリに配置されたかのテスト
def groupId = "tpanda.maven.plugins.it"
def artifactId = "jni-it"
def version = "1.0-SNAPSHOT"
def classifier = "jni-win"
def extension = "dll"

// ローカルリポジトリ内のパスを構築
def artifactDir = new File(localRepositoryPath, "${groupId.replace('.', '/')}/${artifactId}/${version}")
def attachedFile = new File(artifactDir, "${artifactId}-${version}-${classifier}.${extension}")

assert attachedFile.exists() : "Attached artifact not found at: ${attachedFile.absolutePath}"
assert attachedFile.length() > 0 : "Attached artifact is empty"

println "Verification successful: ${attachedFile.name} exists in local repo."

