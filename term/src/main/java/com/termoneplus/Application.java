/*
 * Copyright (C) 2018-2020 Roumen Petrov.  All rights reserved.
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
import android.content.res.AssetManager;
import androidx.preference.PreferenceManager;

import java.io.File;


public class Application extends android.app.Application {
    public static final String ID = BuildConfig.APPLICATION_ID;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
    public static final String APP_TAG = "TermOnePlus";

    public static final String NOTIFICATION_CHANNEL_SESSIONS = BuildConfig.APPLICATION_ID + ".sessions";

    public static final String ACTION_OPEN_NEW_WINDOW = "com.termoneplus.OPEN_NEW_WINDOW";
    public static final String ACTION_SWITCH_WINDOW = "com.termoneplus.SWITCH_WINDOW";
    public static final String ACTION_RUN_SHORTCUT = "com.termoneplus.RUN_SHORTCUT";
    public static final String ACTION_RUN_SCRIPT = "com.termoneplus.RUN_SCRIPT";

    public static final String ARGUMENT_TARGET_WINDOW = "target_window";
    public static final String ARGUMENT_WINDOW_ID = "window_id";
    /* arguments for use by external applications */
    public static final String ARGUMENT_SHELL_COMMAND = "com.termoneplus.Command";
    public static final String ARGUMENT_WINDOW_HANDLE = "com.termoneplus.WindowHandle";

    public static Settings settings;

    public static File xbindir;

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
        xbindir = libdir;
        cachedir = getCacheDir();

        setupPreferences();

        Installer.install_directory(etcdir, false);
        install_skeleton();

        // Note at this point xbindir == libdir
        File exe = new File(xbindir, Installer.APPINFO_COMMAND);
        if (!exe.canExecute()) {
            // Old Android (API Level < 17) - libraries are without executable bit set
            xbindir = new File(rootdir, ".x");
            Installer.install_directory(xbindir, false);

            Installer.copy_executable(exe, xbindir);
        }

        Installer.installAppScriptFile();
    }

    private void setupPreferences() {
        boolean updated = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        String pref_home_path = getString(R.string.key_home_path_preference);
        if (!prefs.contains(pref_home_path)) {
            String path = getDir("HOME", MODE_PRIVATE).getAbsolutePath();
            editor.putString(pref_home_path, path);
            updated = true;
        }

        if (updated) editor.apply();

        settings = new Settings(this, prefs);
    }

    private boolean install_skeleton() {
        String asset_path = "skel";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String homedir = prefs.getString(getString(R.string.key_home_path_preference), "");

        AssetManager am = getAssets();
        try {
            String[] list = am.list(asset_path);
            if (list == null) return true;
            for (String item : list)
                if (!install_skeleton(homedir, am, asset_path, item))
                    return false;
        } catch (Exception ignore) {
        }
        return true;
    }

    protected final boolean install_skeleton(String homedir, AssetManager am, String asset_path, String item) {
        File target = new File(homedir, "." + item);

        if (target.exists()) return true;

        return Installer.install_asset(am, asset_path + "/" + item, target);
    }
}
