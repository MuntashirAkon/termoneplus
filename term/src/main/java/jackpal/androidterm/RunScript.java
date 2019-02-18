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

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.termoneplus.Application;


/*
 * New procedure for launching a command in ATE.
 * Build the path and arguments into a Uri and set that into Intent.data.
 * intent.data(new Uri.Builder().setScheme("file").setPath(path).setFragment(arguments))
 *
 * The old procedure of using Intent.Extra is still available but is discouraged.
 */
public final class RunScript extends RemoteInterface {

    @Override
    protected void processAction(@NonNull Intent intent, @NonNull String action) {
        switch (action) {
            case Application.ACTION_RUN_SCRIPT:
                /* Someone with the appropriate permissions has asked us to run a script */
                runScript(intent);
                break;
        }
    }

    private void runScript(@NonNull Intent intent) {
        String command = null;

        // First look in Intent URI (data) for the path; if not there, revert to
        // the "shell command" location.
        Uri uri = intent.getData();
        if (uri != null) {
            String s = uri.getScheme();
            if (s != null) s = s.toLowerCase();
            if (s != null) {
                switch (s) {
                    case "file":
                        command = uri.getPath();
                        if (TextUtils.isEmpty(command)) break;

                        command = quoteForBash(command);

                        // consider scheme fragment as command arguments
                        s = uri.getFragment();
                        if (s != null)
                            command += " " + s;
                        break;
                    // TODO "context" scheme
                }
            }
        }
        if (command == null) {
            command = intent.getStringExtra(Application.ARGUMENT_SHELL_COMMAND);
            /* TODO: use of quoteForBash()
               Call of quoteForBash() was added by commit
               "rewrite processing on RunScript similarly to RunShortcut"
               but it breaks intents sample "Run Script".
            if (command != null)
                command = quoteForBash(command);
            */
        }

        if (command == null) {
            Log.e(Application.APP_TAG, "No command provided in script!");
            return;
        }

        String handle = intent.getStringExtra(Application.ARGUMENT_WINDOW_HANDLE);
        if (handle != null) {
            // Target the request at an existing window if open
            handle = appendToWindow(handle, command);
        } else {
            // Open a new window
            handle = openNewWindow(command);
        }

        Intent result = new Intent();
        result.putExtra(Application.ARGUMENT_WINDOW_HANDLE, handle);
        setResult(RESULT_OK, result);
    }
}
