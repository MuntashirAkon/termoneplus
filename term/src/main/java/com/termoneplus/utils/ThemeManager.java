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

import com.termoneplus.R;


public class ThemeManager {
    public static final String PREF_THEME_MODE = "thememode";

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
