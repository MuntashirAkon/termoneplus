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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;


public class Permissions {

    public static final int REQUEST_EXTERNAL_STORAGE = 101;

    static String[] external_stogare_permissions = null;


    public static void constructExternalStoragePermissions() {
        if (external_stogare_permissions != null) return;

        ArrayList<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN /*API Level 16*/) {
            // added in API level 16
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        external_stogare_permissions = list.toArray(new String[0]);
    }

    public static boolean permissionExternalStorage(AppCompatActivity activity) {
        boolean granted = true;
        constructExternalStoragePermissions();
        for (String permission : external_stogare_permissions) {
            int status = ActivityCompat.checkSelfPermission(activity, permission);
            if (status == PackageManager.PERMISSION_GRANTED) continue;
            granted = false;
            break;
        }
        return granted;
    }

    public static boolean shouldShowExternalStorageRationale(AppCompatActivity activity) {
        boolean flag = false;
        constructExternalStoragePermissions();
        for (String permission : external_stogare_permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static void requestPermissionExternalStorage(AppCompatActivity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, external_stogare_permissions, requestCode);
    }

    public static boolean isPermissionGranted(int[] grantResults) {
        // Note if request is cancelled, the result arrays are empty.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return grantResults.length > 0; // i.e. false by default
    }
}
