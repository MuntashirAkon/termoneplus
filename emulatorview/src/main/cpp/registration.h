#ifndef TERMONEPLUS_REGISTRATION_H
#define TERMONEPLUS_REGISTRATION_H
/*
 * Copyright (C) 2018 Roumen Petrov.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <android/log.h>


int register_native(
        JNIEnv *env, const char *class_name,
        JNINativeMethod *methods, size_t num_methods
);


void throwOutOfMemoryError(JNIEnv *env, const char *msg) ;
void throwIOException(JNIEnv *env, const char *msg);


int termoneplus_log_print(int prio, const char *fmt, ...);
#define LOGE(...) do { termoneplus_log_print(ANDROID_LOG_ERROR, __VA_ARGS__); } while(0)

#endif /* ndef TERMONEPLUS_REGISTRATION_H */
