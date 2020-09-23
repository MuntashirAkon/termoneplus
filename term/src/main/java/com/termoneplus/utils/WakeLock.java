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

package com.termoneplus.utils;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.termoneplus.Application;
import com.termoneplus.R;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;


public class WakeLock {
    private static PowerManager.WakeLock lock = null;

    public static void create(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        try {
            lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Application.APP_TAG + ":");
        } catch (Exception ignore) {
        }
    }

    public static void release() {
        if (lock == null) return;

        if (lock.isHeld()) lock.release();

        lock = null;
    }

    public static boolean isHeld() {
        return (lock != null) && lock.isHeld();
    }

    @SuppressLint("WakelockTimeout")
    public static void toggle(Context context) {
        if (lock == null) return;

        if (lock.isHeld()) {
            lock.release();
        } else {
            lock.acquire();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M /*API Level 23*/)
                batteryOptimizations(context);
        }
    }

    @RequiresApi(23)
    private static void batteryOptimizations(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;

        String package_name = context.getPackageName();
        if (pm.isIgnoringBatteryOptimizations(package_name)) return;

        Resources res = context.getResources();
        String app_name = res.getString(R.string.application_terminal);
        String msg = res.getString(R.string.ignore_battery_optimizations, app_name);

        new AlertDialog.Builder(context)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> openPowerSettings(context)
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    @RequiresApi(23)
    private static void openPowerSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignore) {
        }
    }
}
