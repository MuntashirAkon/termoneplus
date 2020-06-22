/*
 * Copyright (C) 2019-2020 Roumen Petrov.  All rights reserved.
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

import android.content.res.AssetManager;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;


public class Installer {

    public static final String APPINFO_COMMAND;

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN /*API level 16*/)
            APPINFO_COMMAND = "libexeo-t1plus.so";
        else
            APPINFO_COMMAND = "libexec-t1plus.so";
    }

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
        ArrayList<String> shell_script = new ArrayList<>();

        String sysmkshrc = "/system/etc/mkshrc";
        if (!Application.getScriptFilePath().equals(sysmkshrc) &&
                Application.settings.sourceSystemShellStartupFile() &&
                new File(sysmkshrc).exists())
            shell_script.add(". " + sysmkshrc);

        // Source application startup script
        shell_script.add(". ~/.shrc");

        //Next work fine with mksh but fail with ash.
        //shell_script.add(". /proc/self/fd/0 <<< \"$(libexec-t1plus.so aliases)\"");
        shell_script.add(". /proc/self/fd/0 <<EOF");
        shell_script.add("$(" + APPINFO_COMMAND + " aliases)");
        shell_script.add("EOF");

        return install_text_file(shell_script.toArray(new String[0]), Application.getScriptFile());
    }

    public static boolean copy_executable(File source, File target_path) {
        int buflen = 32 * 1024; // 32k
        byte[] buf = new byte[buflen];

        File target = new File(target_path, source.getName());
        File backup = new File(target.getAbsolutePath() + "-bak");
        if (target.exists())
            if (!target.renameTo(backup))
                return false;

        try {
            OutputStream os = new FileOutputStream(target);
            InputStream is = new FileInputStream(source);
            int len;
            while ((len = is.read(buf, 0, buflen)) > 0) {
                os.write(buf, 0, len);
            }
            os.close();
            is.close();

            if (backup.exists())
                backup.delete();

            // always preset executable permissions
            return target.setReadable(true) &&
                    target.setExecutable(true, false);
        } catch (Exception ignore) {
        }
        return false;
    }

    public static boolean install_asset(AssetManager am, String asset, File target) {
        int buflen = 32 * 1024; // 32k
        byte[] buf = new byte[buflen];

        try {
            OutputStream os = new FileOutputStream(target);
            InputStream is = am.open(asset, AssetManager.ACCESS_STREAMING);
            int len;
            while ((len = is.read(buf, 0, buflen)) > 0) {
                os.write(buf, 0, len);
            }
            is.close();
            os.close();

            return true;
        } catch (IOException ignore) {
        }
        return false;
    }
}
