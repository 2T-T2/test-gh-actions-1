#pragma once

#ifdef BUILD_MYMATH
#define API(T) __declspec(dllexport) T __stdcall
#else
#define API(T) __declspec(dllimport) T __stdcall
#endif

#ifdef __cplusplus
extern "C" {
#endif
    API(int) add(int a, int b);
    API(int) sub(int a, int b);
    API(int) mul(int a, int b);
    API(int) div(int a, int b);
#ifdef __cplusplus
}
#endif
