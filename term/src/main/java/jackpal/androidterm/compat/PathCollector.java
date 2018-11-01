/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2017-2018 Roumen Petrov.  All rights reserved.
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

package jackpal.androidterm.compat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

import jackpal.androidterm.util.TermSettings;


/* NOTE: refactored broadcast functionality from Term.java
 * Applications that target Android 8.0 (Oreo, API Level 26) or higher no
 * longer receive implicit broadcasts registered in their manifest.
 * Broadcast registered at run-time are excluded but we would like
 * to receive paths from all application not only from running.
 * TODO: pending removal of deprecated path collection based on broadcasts.
 */
@Deprecated
public class PathCollector {
    private static final String ACTION_PATH_APPEND_BROADCAST = "com.termoneplus.broadcast.APPEND_TO_PATH";
    private static final String ACTION_PATH_PREPEND_BROADCAST = "com.termoneplus.broadcast.PREPEND_TO_PATH";
    private static final String PERMISSION_PATH_APPEND_BROADCAST = "com.termoneplus.permission.APPEND_TO_PATH";
    private static final String PERMISSION_PATH_PREPEND_BROADCAST = "com.termoneplus.permission.PREPEND_TO_PATH";

    private int pending;
    private OnPathsReceivedListener callback;

    public PathCollector(AppCompatActivity context, PathSettings settings) {
        pending = 0;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                String path = makePathFromBundle(getResultExtras(false));
                switch (action) {
                    case ACTION_PATH_PREPEND_BROADCAST:
                        settings.setPrependPath(path);
                        break;
                    case ACTION_PATH_APPEND_BROADCAST:
                        settings.setAppendPath(path);
                        break;
                    default:
                        return;
                }
                --pending;

                if (pending <= 0 && callback != null)
                    callback.onPathsReceived();
            }
        };

        ++pending;
        Intent broadcast = new Intent(ACTION_PATH_APPEND_BROADCAST);
        broadcast.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendOrderedBroadcast(broadcast, PERMISSION_PATH_APPEND_BROADCAST,
                receiver, null, AppCompatActivity.RESULT_OK, null, null);

        ++pending;
        broadcast = new Intent(broadcast);
        broadcast.setAction(ACTION_PATH_PREPEND_BROADCAST);
        context.sendOrderedBroadcast(broadcast, PERMISSION_PATH_PREPEND_BROADCAST,
                receiver, null, AppCompatActivity.RESULT_OK, null, null);
    }

    private static String makePathFromBundle(Bundle extras) {
        if (extras == null || extras.size() == 0)
            return "";

        String[] keys = new String[extras.size()];
        keys = extras.keySet().toArray(keys);
        Collator collator = Collator.getInstance(Locale.US);
        Arrays.sort(keys, collator);

        StringBuilder path = new StringBuilder();
        for (String key : keys) {
            String dir = extras.getString(key);
            if (dir != null && !dir.equals("")) {
                path.append(dir);
                path.append(":");
            }
        }

        return path.substring(0, path.length() - 1);
    }

    public void setOnPathsReceivedListener(OnPathsReceivedListener listener) {
        callback = listener;
    }

    public interface OnPathsReceivedListener {
        void onPathsReceived();
    }
}
