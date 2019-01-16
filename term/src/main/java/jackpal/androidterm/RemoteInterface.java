/*
 * Copyright (C) 2012 Steven Luo
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

package jackpal.androidterm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.termoneplus.Application;
import com.termoneplus.TermActivity;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import jackpal.androidterm.compat.PathSettings;
import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.util.SessionList;
import jackpal.androidterm.util.TermSettings;


public class RemoteInterface extends AppCompatActivity {
    private TermSettings mSettings;
    private PathSettings path_settings;

    private TermService mTermService;
    private Intent mTSIntent;
    private ServiceConnection mTSConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mTermService = null;
            if (service != null) {
                TermService.TSBinder binder = (TermService.TSBinder) service;
                mTermService = binder.getService();
            }
            handleIntent();
        }

        public void onServiceDisconnected(ComponentName className) {
            mTermService = null;
        }
    };

    /**
     * Quote a string so it can be used as a parameter in bash and similar shells.
     */
    public static String quoteForBash(String s) {
        StringBuilder builder = new StringBuilder();
        String specialChars = "\"\\$`!";
        builder.append('"');
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (specialChars.indexOf(c) >= 0) {
                builder.append('\\');
            }
            builder.append(c);
        }
        builder.append('"');
        return builder.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new TermSettings(getResources(), prefs);
        path_settings = new PathSettings(getResources(), prefs);

        Intent TSIntent = new Intent(this, TermService.class);
        mTSIntent = TSIntent;
        startService(TSIntent);
        if (!bindService(TSIntent, mTSConnection, BIND_AUTO_CREATE)) {
            Log.e(Application.APP_TAG, "bind to service failed!");
            finish();
        }
    }

    @Override
    public void finish() {
        ServiceConnection conn = mTSConnection;
        if (conn != null) {
            unbindService(conn);

            // Stop the service if no terminal sessions are running
            TermService service = mTermService;
            if (service != null) {
                SessionList sessions = service.getSessions();
                if (sessions == null || sessions.size() == 0) {
                    stopService(mTSIntent);
                }
            }

            mTSConnection = null;
            mTermService = null;
        }
        super.finish();
    }

    protected TermService getTermService() {
        return mTermService;
    }

    protected void handleIntent() {
        TermService service = getTermService();
        Intent intent = null;
        String action = null;

        if (service != null) intent = getIntent();
        if (intent != null) action = intent.getAction();
        if (intent != null)
            processAction(intent, action);

        finish();
    }

    private void processAction(@NonNull Intent intent, String action) {
        Log.i(Application.APP_TAG, "RemoteInterface action: " + action);
        if (Intent.ACTION_SEND.equals(action)) {
            /* "permission.RUN_SCRIPT" not required as this is merely opening a new window. */
            processSendAction(intent);
            return;
        }
        // Intent sender may not have permissions, ignore any extras
        openNewWindow(null);
    }

    private void processSendAction(@NonNull Intent intent) {
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            Object extraStream = intent.getExtras().get(Intent.EXTRA_STREAM);
            if (extraStream instanceof Uri) {
                String path = ((Uri) extraStream).getPath();
                File file = new File(path);
                String dirPath = file.isDirectory() ? path : file.getParent();
                openNewWindow("cd " + quoteForBash(dirPath));
                return;
            }
        }
        openNewWindow(null);
    }

    protected String openNewWindow(String iInitialCommand) {
        TermService service = getTermService();

        String initialCommand = mSettings.getInitialCommand();
        if (iInitialCommand != null) {
            if (initialCommand != null) {
                initialCommand += "\r" + iInitialCommand;
            } else {
                initialCommand = iInitialCommand;
            }
        }

        try {
            TermSession session = TermActivity.createTermSession(this, mSettings, path_settings, initialCommand);

            session.setFinishCallback(service);
            service.getSessions().add(session);

            String handle = UUID.randomUUID().toString();
            ((GenericTermSession) session).setHandle(handle);

            Intent intent = new Intent(this, TermActivity.class)
                    .setAction(Application.ACTION_OPEN_NEW_WINDOW)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            return handle;
        } catch (IOException e) {
            return null;
        }
    }

    protected String appendToWindow(String handle, String iInitialCommand) {
        TermService service = getTermService();

        // Find the target window
        SessionList sessions = service.getSessions();
        GenericTermSession target = null;
        int index;
        for (index = 0; index < sessions.size(); ++index) {
            GenericTermSession session = (GenericTermSession) sessions.get(index);
            String h = session.getHandle();
            if (h != null && h.equals(handle)) {
                target = session;
                break;
            }
        }

        if (target == null) {
            // Target window not found, open a new one
            return openNewWindow(iInitialCommand);
        }

        if (iInitialCommand != null) {
            target.write(iInitialCommand);
            target.write('\r');
        }

        Intent intent = new Intent(this, TermActivity.class)
                .setAction(Application.ACTION_SWITCH_WINDOW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Application.ARGUMENT_TARGET_WINDOW, index);
        startActivity(intent);

        return handle;
    }
}
