/*
 * Copyright (C) 2017-2019 Roumen Petrov.  All rights reserved.
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

package com.termoneplus.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.termoneplus.Application;
import com.termoneplus.R;
import com.termoneplus.utils.TextIcon;

import java.security.GeneralSecurityException;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import jackpal.androidterm.RemoteInterface;
import jackpal.androidterm.RunShortcut;
import jackpal.androidterm.compat.PRNGFixes;
import jackpal.androidterm.util.ShortcutEncryption;


public class AddShortcut extends AppCompatActivity {
    private final int REQUEST_FIND_COMMAND = 101;

    private View shortcut_view;
    private SharedPreferences preferences;
    private String path = "";
    private String iconText[] = {"", null};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        setContentView(R.layout.activity_add2);
        */
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String action = getIntent().getAction();
        if (action != null && action.equals("android.intent.action.CREATE_SHORTCUT"))
            makeShortcut();
        else
            finish();
    }

    private void makeShortcut() {

        LayoutInflater inflater = getLayoutInflater();
        shortcut_view = inflater.inflate(R.layout.activity_addshortcut, null);

        final EditText cmd_param = shortcut_view.findViewById(R.id.cmd_param);
        final EditText cmd_name = shortcut_view.findViewById(R.id.cmd_name);

        cmd_param.setOnFocusChangeListener((view, focus) -> {
            if (!focus) {
                String s;
                if (cmd_name.getText().toString().equals("") &&
                        !(s = cmd_param.getText().toString()).equals("")
                        )
                    cmd_name.setText(s.split("\\s")[0]);
            }
        });

        Button btn_cmd_path = shortcut_view.findViewById(R.id.btn_cmd_path);
        btn_cmd_path.setOnClickListener(
                view -> {
                    Intent pickerIntent = new Intent(Intent.ACTION_PICK)
                            .putExtra("CONTENT_TYPE", "*/*");

                    pickerIntent.putExtra("TITLE", getString(R.string.addshortcut_button_find_command));

                    String lastPath = preferences.getString("lastPath", null);
                    if (lastPath != null)
                        pickerIntent.putExtra("COMMAND_PATH", lastPath);

                    try {
                        startActivityForResult(pickerIntent, REQUEST_FIND_COMMAND);
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Failed to launch pick action!", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
        );

        final ImageView cmd_icon = shortcut_view.findViewById(R.id.cmd_icon);
        cmd_icon.setTag(0xFFFFFFFF);

        Button btn_cmd_icon = shortcut_view.findViewById(R.id.btn_cmd_icon);
        btn_cmd_icon.setOnClickListener(
                view -> new ColorValue(AddShortcut.this, cmd_icon, iconText));

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setView(shortcut_view);
        alert.setTitle(getString(R.string.addshortcut_title));
        alert.setPositiveButton(android.R.string.ok,
                (dialog, which) -> buildShortcut(
                        AddShortcut.this,
                        path,
                        cmd_param.getText().toString(),
                        cmd_name.getText().toString(),
                        iconText[1],
                        (Integer) cmd_icon.getTag()
                )
        );
        alert.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> finish()
        );
        alert.show();
    }

    private void buildShortcut(
            Context context,
            String path,
            String arguments,
            String shortcutName,
            String shortcutText,
            int shortcutColor
    ) {
        // Apply workarounds for SecureRandom bugs in Android < 4.4
        PRNGFixes.apply();

        ShortcutEncryption.Keys keys = ShortcutEncryption.getKeys(context);
        if (keys == null) {
            try {
                keys = ShortcutEncryption.generateKeys();
            } catch (GeneralSecurityException e) {
                Log.e(Application.APP_TAG, "Generating shortcut encryption keys failed: " + e.toString());
                throw new RuntimeException(e);
            }
            ShortcutEncryption.saveKeys(context, keys);
        }

        StringBuilder cmd = new StringBuilder();
        if (!TextUtils.isEmpty(path)) cmd.append(RemoteInterface.quoteForBash(path));
        if (!TextUtils.isEmpty(arguments)) cmd.append(" ").append(arguments);
        String cmdStr = cmd.toString();
        String cmdEnc = null;

        try {
            cmdEnc = ShortcutEncryption.encrypt(cmdStr, keys);
        } catch (GeneralSecurityException e) {
            Log.e(Application.APP_TAG, "Shortcut encryption failed: " + e.toString());
            throw new RuntimeException(e);
        }

        Intent target = new Intent().setClass(context, RunShortcut.class);
        target.setAction(Application.ACTION_RUN_SHORTCUT);
        target.putExtra(Application.ARGUMENT_SHELL_COMMAND, cmdEnc);
        target.putExtra(Application.ARGUMENT_WINDOW_HANDLE, shortcutName);
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent wrapper = new Intent();
        wrapper.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        wrapper.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);
        if (!TextUtils.isEmpty(shortcutName)) {
            wrapper.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        }

        Bitmap icon = null;
        if (!TextUtils.isEmpty(shortcutText))
            icon = TextIcon.create(context, shortcutText, shortcutColor);
        if (icon != null)
            wrapper.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        else
            wrapper.putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher)
            );

        setResult(RESULT_OK, wrapper);
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_FIND_COMMAND: {
                if (resultCode != RESULT_OK) break;
                if (data == null) break;

                {
                    Uri uri = data.getData();
                    if (uri == null) break;
                    path = uri.getPath();
                }
                if (path == null) {
                    finish();
                    break;
                }

                preferences.edit().putString("lastPath", path).commit();

                {
                    EditText cmd_path = shortcut_view.findViewById(R.id.cmd_path);
                    cmd_path.setText(path);
                }

                String name = path.replaceAll(".*/", "");

                {
                    EditText cmd_name = shortcut_view.findViewById(R.id.cmd_name);
                    if (cmd_name.getText().toString().equals(""))
                        cmd_name.setText(name);
                }

                if (iconText[0] != null && iconText[0].equals(""))
                    iconText[0] = name;

                break;
            }
        }
    }
}
