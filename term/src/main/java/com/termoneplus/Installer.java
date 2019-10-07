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

package com.termoneplus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Installer {

    public static boolean install_directory(File dir, boolean share) {
        if (!(dir.exists() || dir.mkdir())) return false;

        // always preset directory permissions
        return dir.setReadable(true, !share) &&
                dir.setExecutable(true, false);
    }

    public static boolean install_text_file(String[] script, File file) {
        try {
            PrintWriter out = new PrintWriter(file);
            for (String line : script)
                out.println(line);
            out.flush();
            out.close();
            // always preset permissions
            return file.setReadable(true, true);
        } catch (IOException ignore) {
        }
        return false;
    }

    public static boolean installAppScriptFile() {
        File file = Application.getScriptFile();

        String[] shell_script = {
                ". /system/etc/mkshrc",
                ". /proc/self/fd/0 <<< \"$(libexec-t1plus.so aliases)\""
        };
        return install_text_file(shell_script, file);
    }
}
