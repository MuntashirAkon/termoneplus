/*
 * Copyright (C) 2018 Roumen Petrov.  All rights reserved.
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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import jackpal.androidterm.R;
import jackpal.androidterm.Term;
import jackpal.androidterm.TermDebug;
import jackpal.androidterm.TermService;
import jackpal.androidterm.util.SessionList;


public class WindowListActivity extends AppCompatActivity
        implements WindowListFragment.OnItemSelectedListener {

    private final ServiceConnection service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TermService.TSBinder binder = (TermService.TSBinder) service;
            TermService term_service = binder.getService();
            SessionList sessions = term_service.getSessions();
            setSessions(sessions);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_windowlist);

        setResult(RESULT_CANCELED);

        {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        }
        {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }
        {
            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(view -> onPositionSelected(-1));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent TSIntent = new Intent(this, TermService.class);
        if (!bindService(TSIntent, service_connection, BIND_AUTO_CREATE)) {
            Log.e(TermDebug.LOG_TAG, "bind to service failed!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        setSessions(null);
        unbindService(service_connection);
    }

    @Override
    public void onPositionSelected(int position) {
        Intent data = new Intent();
        data.putExtra(Term.EXTRA_WINDOW_ID, position);
        setResult(RESULT_OK, data);
        finish();
    }

    private void setSessions(SessionList sessions) {
        FragmentManager manager = getSupportFragmentManager();
        WindowListFragment fragment = (WindowListFragment) manager.findFragmentById(R.id.windowlist);
        WindowListAdapter adapter = (WindowListAdapter) fragment.getListAdapter();
        adapter.setSessions(sessions);
    }
}
