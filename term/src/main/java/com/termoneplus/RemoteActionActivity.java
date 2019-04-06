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

package com.termoneplus;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import jackpal.androidterm.TermService;
import jackpal.androidterm.compat.PathCollector;
import jackpal.androidterm.compat.PathSettings;
import jackpal.androidterm.util.TermSettings;


public class RemoteActionActivity extends AppCompatActivity {
    protected TermSettings mSettings;
    protected PathSettings path_settings;

    private boolean path_collected = false;
    private TermService term_service = null;

    private Intent service_intent;
    private ServiceConnection service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            term_service = null;
            if (service == null) return;

            TermService.TSBinder binder = (TermService.TSBinder) service;
            term_service = binder.getService();

            processIntent();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            term_service = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* intent is required */
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new TermSettings(getResources(), pref);

        path_settings = new PathSettings(getResources(), pref);
        PathCollector path_collector = new PathCollector(this, path_settings);
        path_collector.setOnPathsReceivedListener(() -> {
            path_collected = true;

            processIntent();
        });

        service_intent = new Intent(this, TermService.class);
        startService(service_intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!bindService(service_intent, service_connection, BIND_AUTO_CREATE)) {
            Log.e(Application.APP_TAG, "bind to service failed!");
            finish();
        }
    }

    @Override
    protected void onStop() {
        unbindService(service_connection);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (term_service != null) {
            if (term_service.getSessionCount() == 0)
                stopService(service_intent);
            term_service = null;
        }
        super.onDestroy();
    }

    protected TermService getTermService() {
        return term_service;
    }

    protected void processAction(@NonNull Intent intent, @NonNull String action) {
        //nop, override at child level
    }

    private void processIntent() {
        /* process intent after path collection and start of service */
        if (term_service == null) return;
        if (!path_collected) return;

        /* intent is required - see onCreate() */
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null)
            processAction(intent, action);

        finish();
    }
}
