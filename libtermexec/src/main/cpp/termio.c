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

#include "registration.h"

#if defined(__cplusplus)
# error "__cplusplus"
#endif

#include <pty.h>
#include <errno.h>
#include <stdio.h>


static void
termios_setUTF8Input(
        JNIEnv *env, jobject clazz,
        jint fd, jboolean flag
) {
    struct termios tio;

    (void) clazz;

    if (tcgetattr(fd, &tio) != 0) {
        throwIOException(env, "Failed to get the parameters associated with the terminal");
        return;
    }

/* quoted from termios(3) manual page:
 * c_iflag flag constants:
 * ...
 * IUTF8 (since Linux 2.6.4)
 *   (not in POSIX) Input is UTF8; this allows character-erase to be correctly performed in cooked mode.
 */
    if (flag) {
        tio.c_iflag |= IUTF8;
    } else {
        tio.c_iflag &= ~IUTF8;
    }

    if (tcsetattr(fd, TCSANOW, &tio) != 0) {
        throwIOException(env, "Failed to set the parameters associated with the terminal");
    }
}


static void
ioctl_setWindowSize(
        JNIEnv *env, jobject clazz,
        jint fd, jint row, jint col, jint xpixel, jint ypixel
) {
    struct winsize arg;

    (void) clazz;

    arg.ws_row = (unsigned short) row;
    arg.ws_col = (unsigned short) col;
    arg.ws_xpixel = (unsigned short) xpixel;
    arg.ws_ypixel = (unsigned short) ypixel;

/* quoted from tty_ioctl(4) manual page:
 * TIOCSWINSZ     const struct winsize *argp
 *   Set window size.
 * ...
 * When the window size changes, a SIGWINCH signal is sent to the foreground process group.
 */
    if (ioctl(fd, TIOCSWINSZ, &arg) != 0) {
        char msg[1024];

        snprintf(msg, sizeof(msg), "ioctl: set tty window size fail with error code %d", errno);
        throwIOException(env, msg);
    }
}


int
register_termio(JNIEnv *env) {
    static JNINativeMethod methods[] = {
            {"setUTF8Input", "(IZ)V", (void *) termios_setUTF8Input},
            {"setWindowSize", "(IIIII)V", (void *) ioctl_setWindowSize}
    };
    return register_native(
            env,
            "com/termoneplus/TermIO$Native",
            methods, sizeof(methods) / sizeof(*methods)
    );
}
