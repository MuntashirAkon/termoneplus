##
# Copyright (C) 2018 Roumen Petrov.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##

# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 2.4)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library(
        # Specifies the name of the library.
        emulator-system

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        main/cpp/registration.c
        main/cpp/process.c
        main/cpp/termio.c
        main/cpp/compat.c
)

find_library(
        # Defines the name of the path variable that stores the
        # location of the NDK library.
        log-lib

        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log
)

# Links your native library against one or more other native libraries.
target_link_libraries(
        # Specifies the target library.
        emulator-system

        # Links the log library to the target library.
        ${log-lib}
)
