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

package com.termoneplus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

import com.termoneplus.R;

import java.io.File;


public class ThemeManager {
    public static final String PREF_THEME_MODE = "thememode";
    private static final String PREFERENCES_FILE = "file_selection"; /*obsolete*/
    private static final String PREFERENCE_LIGHT_THEME = "light_theme";  /*obsolete*/

    public static void migrateFileSelectionThemeMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);

        if (!preferences.contains(PREFERENCE_LIGHT_THEME)) return;

        boolean light_theme = preferences.getBoolean(PREFERENCE_LIGHT_THEME, false);

        Toast toast = Toast.makeText(context.getApplicationContext(),
                "Migrate \"File Selection\" theme mode", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        preferences.edit().remove(PREFERENCE_LIGHT_THEME).commit();
        // Note obsolete "FileSelection" preferences have only one item - light_theme!
        {
            File prefs_path = new File(context.getFilesDir().getParentFile(), "shared_prefs");
            for (String name : prefs_path.list((dir, name) -> name.startsWith(PREFERENCES_FILE)))
                //noinspection ResultOfMethodCallIgnored
                new File(prefs_path, name).delete();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        if (light_theme)
            prefs_editor.putString(PREF_THEME_MODE, "light");
        else
            prefs_editor.putString(PREF_THEME_MODE, "dark");
        prefs_editor.apply();
    }

    public static int presetTheme(Context context, boolean actionbar, int resid) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = sharedPreferences.getString(PREF_THEME_MODE, "");

        if (mode.equals(""))
            mode = context.getResources().getString(R.string.pref_thememode_default);

        switch (mode) {
            case "dark":
                resid = actionbar ? R.style.AppTheme
                        : R.style.AppTheme_NoActionBar;
                break;
            case "light":
                resid = actionbar ? R.style.AppTheme_Light
                        : R.style.AppTheme_Light_NoActionBar;
                break;
            case "system":
                resid = actionbar ? R.style.AppTheme_DayNight
                        : R.style.AppTheme_DayNight_NoActionBar;
                break;
        }

        return resid;
    }
}
