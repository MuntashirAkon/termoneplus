/*
 * Copyright (C) 2018-2019 Roumen Petrov.  All rights reserved.
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

import android.content.SharedPreferences;

import com.termoneplus.utils.ThemeManager;

import java.io.File;

import androidx.preference.PreferenceManager;


public class Application extends android.app.Application {
    public static final String ID = BuildConfig.APPLICATION_ID;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
    public static final String APP_TAG = "TermOnePlus";

    public static final String ACTION_OPEN_NEW_WINDOW = "com.termoneplus.OPEN_NEW_WINDOW";
    public static final String ACTION_SWITCH_WINDOW = "com.termoneplus.SWITCH_WINDOW";
    public static final String ACTION_RUN_SHORTCUT = "com.termoneplus.RUN_SHORTCUT";
    public static final String ACTION_RUN_SCRIPT = "com.termoneplus.RUN_SCRIPT";

    public static final String ARGUMENT_TARGET_WINDOW = "target_window";
    public static final String ARGUMENT_WINDOW_ID = "window_id";
    /* arguments for use by external applications */
    public static final String ARGUMENT_SHELL_COMMAND = "com.termoneplus.Command";
    public static final String ARGUMENT_WINDOW_HANDLE = "com.termoneplus.WindowHandle";

    private static File rootdir;
    private static File etcdir;
    private static File libdir;
    private static File cachedir;


    public static File getTmpDir() {
        return cachedir;
    }

    public static String getTmpPath() {
        return getTmpDir().getAbsolutePath();
    }

    public static File getLibDir() {
        return libdir;
    }

    public static String getLibPath() {
        return getLibDir().getPath();
    }

    public static File getScriptFile() {
        return new File(etcdir, "mkshrc");
    }

    public static String getScriptFilePath() {
        return getScriptFile().getPath();
    }


    @Override
    public void onCreate() {
        super.onCreate();

        rootdir = getFilesDir().getParentFile();
        etcdir = new File(rootdir, "etc");
        libdir = new File(getApplicationInfo().nativeLibraryDir);
        cachedir = getCacheDir();

        setupPreferences();
        ThemeManager.migrateFileSelectionThemeMode(this);

        Installer.install_directory(etcdir, false);
        Installer.installAppScriptFile();
    }

    private void setupPreferences() {
        boolean updated = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        if (!prefs.contains("home_path")) {
            String path = getDir("HOME", MODE_PRIVATE).getAbsolutePath();
            editor.putString("home_path", path);
            updated = true;
        }
        // remove obsolete preferences:
        // - after v3.0.0, TODO remove string resources in next major
        if (prefs.contains("allow_prepend_path")) {
            // Note depends from do_path_extensions
            editor.remove("allow_prepend_path");
            updated = true;
        }
        if (prefs.contains("do_path_extensions")) {
            editor.remove("do_path_extensions");
            updated = true;
        }

        if (updated) editor.apply();
    }
}
