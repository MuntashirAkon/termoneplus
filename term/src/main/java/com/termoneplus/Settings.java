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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;

import jackpal.androidterm.emulatorview.ColorScheme;


public class Settings {
    // foreground and background as ARGB color pair
    /* Note keep synchronized with names in @array.entries_color_preference
    and index in @array.entryvalues_color_preference. */
    public static final ColorScheme[] color_schemes = {
            new ColorScheme(0xFF000000, 0xFFFFFFFF, false) /*black on white*/,
            new ColorScheme(0xFFFFFFFF, 0xFF000000, true) /*white on black*/,
            new ColorScheme(0xFFFFFFFF, 0xFF344EBD, true) /*white on blue*/,
            new ColorScheme(0xFF00FF00, 0xFF000000, true) /*green on black*/,
            new ColorScheme(0xFFFFB651, 0xFF000000, true) /*amber on black*/,
            new ColorScheme(0xFFFF0113, 0xFF000000, true) /*red on black*/,
            new ColorScheme(0xFF33B5E5, 0xFF000000, true) /*holo-blue on black*/,
            new ColorScheme(0xFF657B83, 0xFFFDF6E3, false) /*solarized light*/,
            new ColorScheme(0xFF839496, 0xFF002B36, true) /*solarized dark*/,
            new ColorScheme(0xFFAAAAAA, 0xFF000000, true) /*linux console*/,
            new ColorScheme(0xFFDCDCCC, 0xFF2C2C2C, true) /*dark pastels*/
    };

    private static final String SOURCE_SYS_SHRC_KEY = "source_sys_shrc";

    private boolean source_sys_shrc;


    public Settings(Context context, SharedPreferences preferences) {
        Resources r = context.getResources();
        source_sys_shrc = parseBoolean(preferences,
                context.getString(R.string.key_source_sys_shrc_preference),
                r.getBoolean(R.bool.pref_source_sys_shrc_default));
    }

    public boolean parsePreference(Context context, SharedPreferences preferences, String key) {
        if (TextUtils.isEmpty(key)) return false;

        return parseSourceSysRC(context, preferences, key);
    }

    public boolean sourceSystemShellStartupFile() {
        return source_sys_shrc;
    }

    private boolean parseBoolean(SharedPreferences preferences, String key, boolean def) {
        try {
            return preferences.getBoolean(key, def);
        } catch (Exception ignored) {
        }
        return def;
    }

    private boolean parseSourceSysRC(Context context, SharedPreferences preferences, String key) {
        String pref = context.getString(R.string.key_source_sys_shrc_preference);
        if (!key.equals(pref)) return false;

        boolean value = parseBoolean(preferences, key, source_sys_shrc);
        if (value != source_sys_shrc) {
            source_sys_shrc = value;
            Installer.installAppScriptFile();
        }
        return true;
    }
}
