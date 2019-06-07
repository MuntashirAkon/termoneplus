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

#include "compat.h"

#if defined(__cplusplus)
# error "__cplusplus"
#endif

#include <dirent.h>
#include <stdlib.h>
#include <limits.h>
#include <unistd.h>


void
closefrom(int lowfd) {
    int pws_fd = -1;
    DIR *dirp;

    { /* keep android property workspace open */
        char *pws_env = getenv("ANDROID_PROPERTY_WORKSPACE");
        if (pws_env) {
            /* format "int,int" */
            pws_fd = atoi(pws_env);
        }
    }

    dirp = opendir("/proc/self/fd");
    if (dirp != NULL) {
        int dir_fd = dirfd(dirp);
        struct dirent *dent;
        long fd;
        char *endp;

        for (dent = readdir(dirp); dent != NULL; dent = readdir(dirp)) {
            fd = strtol(dent->d_name, &endp, 10);
            if (
                    (dent->d_name != endp) && (*endp == '\0') &&
                    (fd >= 0) && (fd < INT_MAX) &&
                    (fd >= lowfd) &&
                    (fd != pws_fd) && (fd != dir_fd)
                    )
                (void) close((int) fd);
        }

        (void) closedir(dirp);
    }
}
