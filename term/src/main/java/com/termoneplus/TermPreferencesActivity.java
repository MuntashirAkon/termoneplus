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
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.termoneplus.utils.ConsoleStartupScript;
import com.termoneplus.utils.ThemeManager;

import java.util.Objects;


public class TermPreferencesActivity extends BaseActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.preferences);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, new TermPreferencesFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { // Action bar home button selected
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TermPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
            String pref_home_path = getString(R.string.key_home_path_preference);
            String homedir = prefs.getString(pref_home_path, "");

            String pref_shellrc = getString(R.string.key_shellrc_preference);
            ((EditTextPreference) findPreference(pref_shellrc)).setText(ConsoleStartupScript.read(homedir));

            ListPreference themePref = findPreference(ThemeManager.PREF_THEME_MODE);
            Objects.requireNonNull(themePref).setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeManager.setTheme(requireActivity().getApplicationContext(), Integer.parseInt((String) newValue));
                return true;
            });
        }
    }
}
