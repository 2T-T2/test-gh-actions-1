#include "test_NativeMethods.h"

JNIEXPORT jint JNICALL Java_test_NativeMethods_add(JNIEnv *, jclass, jint a, jint b) {
    return a + b;
}
