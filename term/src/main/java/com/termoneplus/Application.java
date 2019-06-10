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

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!prefs.contains("home_path")) {
            SharedPreferences.Editor editor = prefs.edit();
            String path = getDir("HOME", MODE_PRIVATE).getAbsolutePath();
            editor.putString("home_path", path);
            editor.apply();
        }

        ThemeManager.migrateFileSelectionThemeMode(this);
    }
}
