package tpanda.maven.plugins.gcc;

import java.nio.charset.Charset;
import java.nio.file.Path;

public interface Constructs {
    String CLASSIFIER_HEADERS = "headers";
    String CLASSIFIER_IMPLIB = "imp-lib";

    Charset GCC_CHARSET = Charset.defaultCharset();
    Path DEPENDENCY_HEADER_DIR_RELATIVE = Path.of("dependencies", "header");
    Path DEPENDENCY_IMPLIB_DIR_RELATIVE = Path.of("dependencies", "imp-lib");

    String GOAL_COMPILE_CC = "cc-compile";
    String GOAL_COMPILE_CXX = "cxx-compile";
}
