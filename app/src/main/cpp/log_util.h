
#ifndef MSOPENGL_LOG_UTIL_H
#define MSOPENGL_LOG_UTIL_H
#include <android/log.h>
#define loge(...) __android_log_print(ANDROID_LOG_ERROR,"lpf",__VA_ARGS__);
#endif //MSOPENGL_LOG_UTIL_H
