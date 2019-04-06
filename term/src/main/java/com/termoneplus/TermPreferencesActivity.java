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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.preference.PreferenceFragmentCompat;


public class TermPreferencesActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        {
            ActionBar action_bar = getSupportActionBar();
            if (action_bar != null) {
                action_bar.setDisplayHomeAsUpEnabled(true);
            }
        }

        loadPreferences();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // Action bar home button selected
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadPreferences() {
        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new TermPreferencesFragment())
                .commit();
    }

    public static class TermPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }
}
