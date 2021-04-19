/*
 * Copyright (C) 2020 Roumen Petrov.  All rights reserved.
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

package com.termoneplus.utils;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;


public class ConsoleStartupScript {
    public static String read(String homedir) {
        StringBuilder builder = new StringBuilder();
        final String nl = System.getProperty("line.separator");

        try {
            BufferedReader in = new BufferedReader(new FileReader(getScriptFile(homedir)));
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                builder.append(line).append(nl);
            }
            return builder.toString();
        } catch (IOException ignored) {
        }
        return "# ~/.shrc";
    }

    public static void write(String homedir, String script) {
        if (script == null) return;
        try {
            PrintWriter out = new PrintWriter(getScriptFile(homedir));
            for (String line : script.split("\n"))
                out.println(line);
            out.flush();
            out.close();
        } catch (IOException ignored) {
        }
    }

    public static void rename(String olddir, String newdir) {
        File oldshrc = getScriptFile(olddir);
        if (!oldshrc.exists()) return;

        File shrc = getScriptFile(newdir);
        oldshrc.renameTo(shrc);
    }

    public static File getScriptFile(String homedir) {
        return new File(homedir, ".shrc");
    }

    public static void migrateInitialCommand(String homedir, String cmd) {
        if (TextUtils.isEmpty(cmd)) return;

        try {
            PrintWriter out = new PrintWriter(
                    new FileWriter(getScriptFile(homedir), true));
            out.println("");
            String timestamp = java.text.DateFormat.getDateTimeInstance().format(new Date());
            out.println("# migrated initial command (" + timestamp + "):");
            out.println(cmd);
            out.flush();
            out.close();
        } catch (IOException ignored) {
        }
    }
}
