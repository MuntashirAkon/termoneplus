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
import android.view.View;

import com.termoneplus.utils.ThemeManager;
import com.termoneplus.utils.WrapOpenURL;


public class TermActivity extends jackpal.androidterm.Term {

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // do not process preference "Theme Mode"
        if (ThemeManager.PREF_THEME_MODE.equals(key)) return;

        super.onSharedPreferenceChanged(sharedPreferences, key);
    }

    public void onAppIconClicked(View view) {
        WrapOpenURL.launch(this, urlApplicationSite());
    }

    public void onAppTitleClicked(View view) {
        WrapOpenURL.launch(this, urlApplicationSite());
    }

    public void onEmailAddressClicked(View view) {
        WrapOpenURL.launch(this, urlApplicationMail());
    }

    @Override
    protected void updatePrefs() {
        Integer theme_resid = getThemeId();
        if (theme_resid != null) {
            if (theme_resid != ThemeManager.presetTheme(this, false, theme_resid)) {
                restart(R.string.restart_thememode_change);
                return;
            }
        }
        super.updatePrefs();
    }

    private String urlApplicationSite() {
        return getResources().getString(R.string.application_site);
    }

    private String urlApplicationMail() {
        return "mailto:" + getResources().getString(R.string.application_email);
    }
}
