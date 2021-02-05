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

package com.termoneplus.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import jackpal.androidterm.emulatorview.ColorScheme;

public class ThemeManager {
    public static final String PREF_THEME_MODE = "theme_mode";

    public static void setTheme(@NonNull Context context, @Nullable Integer mode) {
        if (mode == null) {
            mode = getTheme(context);
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int getTheme(@NonNull Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sharedPreferences.getString(PREF_THEME_MODE, "" + AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    public static ColorScheme getColorSchemeMatchingTheme(Context context, ColorScheme colorScheme) {
        if (isNightMode(context) && colorScheme.isNightScheme()) {
            return colorScheme;
        } else {
            // Swap colours
            return new ColorScheme(colorScheme.getBackColor(), colorScheme.getForeColor(),
                    colorScheme.getCursorBackColor(), colorScheme.getCursorForeColor(), false);
        }
    }

    public static boolean isNightMode(@NonNull Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            default:
                return false;
        }
    }
}
