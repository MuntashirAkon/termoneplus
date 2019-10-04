/*
 * Copyright (C) 2019 Roumen Petrov.  All rights reserved.
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

#include <memory.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>

#include "appinfo.h"


static int/*bool*/
get_info(const char *info) {
    int ret = 0;
    char sockname[PATH_MAX + 1];
    char msg[1024];
    char buf[4096];
    int sock;
    size_t len, res;

    if (snprintf(sockname, sizeof(sockname), SOCKET_PREFIX "%ld", (long) getuid())
        >= sizeof(sockname))
        return 0;

    if (snprintf(msg, sizeof(msg), "get %s\n", info) >= sizeof(msg))
        return 0;

    sock = open_socket(sockname);
    if (sock == -1) return 0;

    len = strlen(msg);
    res = atomicio(vwrite, sock, (void *) msg, len);
    if (res != len) goto done;

    while (1) {
        int read_errno;
        errno = 0;
        len = atomicio(read, sock, buf, sizeof(buf));
        read_errno = errno;
        if (len > 0) {
            errno = 0;
            res = atomicio(vwrite, STDOUT_FILENO, buf, len);
            if (res != len) goto done;
        }
        if (read_errno == EPIPE) break;
    }
    (void) fsync(STDOUT_FILENO);

    ret = 1;

    done:
    close(sock);
    return ret;
}


#include <sysexits.h>

int
main(int argc, char *argv[]) {
    int ret;

    if (argc != 2) exit(EX_USAGE);

    ret = get_info(argv[1]);

    return ret ? 0 : EX_SOFTWARE;
}
