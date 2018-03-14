/*
 * Copyright (C) 2017 Roumen Petrov.  All rights reserved.
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

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import jackpal.androidterm.R;
import jackpal.androidterm.WindowListAdapter;
import jackpal.androidterm.util.SessionList;


/**
 * An action bar for terminal emulator activity.
 */

public class TermActionBar {
    private final Toolbar toolbar;
    private final Spinner spinner;

    private TermActionBar(AppCompatActivity context, boolean floating) {
        toolbar = (Toolbar) context.findViewById(R.id.toolbar);
        context.setSupportActionBar(toolbar);

        ActionBar appbar = context.getSupportActionBar();
        if (appbar != null)
            appbar.setDisplayShowTitleEnabled(false);

        spinner = (Spinner) context.findViewById(R.id.spinner);

        if (floating)
            hide();
    }

    public static TermActionBar setTermContentView(AppCompatActivity context, boolean floating) {
        if (floating)
            context.setContentView(R.layout.activity_term_floatbar);
        else
            context.setContentView(R.layout.activity_term);

        return new TermActionBar(context, floating);
    }

    public void setAdapter(WindowListAdapter adapter) {
        spinner.setAdapter(adapter);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        final OnItemSelectedListener callback = listener;
        AdapterView.OnItemSelectedListener wrapper = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                callback.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
        spinner.setOnItemSelectedListener(wrapper);
    }

    public void setSessions(SessionList sessions) {
        WindowListAdapter adapter = (WindowListAdapter) spinner.getAdapter();
        adapter.setSessions(sessions);
    }

    public void setSelection(int position) {
        spinner.setSelection(position);
    }

    public boolean isShowing() {
        return toolbar.getVisibility() == View.VISIBLE;
    }

    public void hide() {
        toolbar.setVisibility(View.GONE);
    }

    public void show() {
        toolbar.setVisibility(View.VISIBLE);
    }

    public void doToggleActionBar() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }
}
