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

package com.termoneplus.utils;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import java.util.List;

public class WrapOpenURL {

    public static void launch(Context context, Uri uri) {

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

        if (activities.size() > 0) {
            try {
                context.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                new AlertDialog.Builder(context)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Failed to launch view action!")
                        .setNeutralButton(android.R.string.ok, null)
                        .create().show();
            }
        } else {
            new AlertDialog.Builder(context)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("Missing view actions!")
                    .setNeutralButton(android.R.string.ok, null)
                    .create().show();
        }
    }

    public static void launch(Context context, String path) {
        Uri uri = Uri.parse(path);
        launch(context, uri);
    }

    public static void launch(Context context, int resId) {
        String path = context.getString(resId);
        launch(context, path);
    }
}
