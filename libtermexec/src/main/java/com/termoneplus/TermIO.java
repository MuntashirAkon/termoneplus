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

package com.termoneplus;

import android.os.ParcelFileDescriptor;

import java.io.IOException;


public class TermIO {

    static {
        System.loadLibrary("term-system");
    }

    public static void setUTF8Input(ParcelFileDescriptor masterPty, boolean flag) throws IOException {
        int fd = masterPty.getFd();
        Native.setUTF8Input(fd, flag);
    }

    public static void setWindowSize(ParcelFileDescriptor masterPty, int row, int col) throws IOException {
        int fd = masterPty.getFd();
        Native.setWindowSize(fd, row, col, 0, 0);
    }


    private static class Native {
        private static native void setUTF8Input(int fd, boolean flag) throws IOException;

        private static native void setWindowSize(int fd, int row, int col, int xpixel, int ypixel) throws IOException;
    }
}
