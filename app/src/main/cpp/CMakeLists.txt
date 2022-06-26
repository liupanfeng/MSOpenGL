# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)
project("msopengl")
#导入头文件
include_directories(include)
# opencv 库文件
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

# 中科院人脸关键点库导入 直接引入  xxx.a库
add_subdirectory(FaceAlignment)
# 导入头文件  include<xxx.h>
include_directories(FaceAlignment/include)



add_library(
             native-lib
             SHARED
             FaceTrack.cpp
             native-lib.cpp )


find_library( # Sets the name of the path variable.
              log-lib

              log )


target_link_libraries(
                       native-lib
                        opencv_java4
                        seeta_fa_lib
                       ${log-lib} )