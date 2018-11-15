/*
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

package com.termoneplus.shortcuts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import jackpal.androidterm.R;


public class FileSelection extends AppCompatActivity {
    private final String PREFERENCES_FILE = "file_selection";
    private final String PREFERENCE_LIGHT_THEME = "light_theme";

    private final String STATE_CWD = "CWD";

    private String cwd; // current working directory


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        {
            SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
            boolean light = preferences.getBoolean(PREFERENCE_LIGHT_THEME, false);

            // override activity theme defined in manifest file
            if (light)
                setTheme(R.style.AppTheme_Light_NoActionBar);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_selection);

        setResult(RESULT_CANCELED);

        {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        }
        {    // Show the Up button in the action bar.
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();

        if (intent.hasExtra("TITLE"))
            setTitle(intent.getStringExtra("TITLE"));

        if (savedInstanceState == null) {
            Uri uri = intent.getData();
            if (uri != null) {
                cwd = uri.getPath();
            }
            if (cwd == null && intent.hasExtra("COMMAND_PATH")) {
                File path = new File(intent.getStringExtra("COMMAND_PATH"));
                if (path.isFile()) path = path.getParentFile();
                if (path.isDirectory())
                    cwd = path.getAbsolutePath();
            }
        } else {
            cwd = savedInstanceState.getString(STATE_CWD);
        }
        if (cwd == null) {
            File path = Environment.getExternalStorageDirectory();
            cwd = path.getAbsolutePath();
        }

        final Adapter adapter = new Adapter(cwd);

        {
            RecyclerView view = findViewById(R.id.directory_list);
            view.setAdapter(adapter);
        }

        {
            final EditText path_input = findViewById(R.id.path);
            path_input.setOnKeyListener(
                    (v, keyCode, event) -> {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            String path = path_input.getText().toString();
                            File file = new File(path);
                            if (!file.exists()) return true;

                            if (file.isDirectory()) {
                                cwd = file.getAbsolutePath();
                                adapter.load(file);
                                adapter.notifyDataSetChanged();
                                return true;
                            }

                            setResult(RESULT_OK, getIntent().setData(Uri.fromFile(file)));
                            finish();
                            return true;
                        }
                        return false;
                    }
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_selection, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // Action bar home/up button selected
                finish();
                return true;
            case R.id.menu_switch_theme:
                onSwitchTheme();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CWD, cwd);
    }

    private void onSwitchTheme() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        boolean light = preferences.getBoolean(PREFERENCE_LIGHT_THEME, false);

        {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("light_theme", !light);
            editor.apply();
        }

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        intent.putExtra("COMMAND_PATH", cwd);
        startActivityForResult(intent, -1);
        finish();
    }

    @IntDef({
            ViewType.ENTRY_PARENT,
            ViewType.ENTRY_DIRECTORY,
            ViewType.ENTRY_FILE,
            ViewType.ENTRY_UNKNOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ViewType {
        int ENTRY_PARENT = 0;
        int ENTRY_DIRECTORY = 1;
        int ENTRY_FILE = 2;
        int ENTRY_UNKNOWN = 9; /*broken symbolic link*/
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;

        public ViewHolder(View view) {
            super(view);
            name = itemView.findViewById(R.id.name);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final View.OnClickListener dir_listener;
        private final View.OnClickListener file_listener;

        private File[] entries;

        public Adapter(String path) {
            {
                File dir = new File(path);
                cwd = dir.getAbsolutePath();
                load(dir);
            }

            dir_listener = view -> {
                String tag = (String) view.getTag();
                if (tag == null) return;

                File dir;
                if (tag.equals("..")) {
                    dir = new File(cwd);
                    // NOTE system does not return parent for root!
                    if (!cwd.equals("/")) dir = dir.getParentFile();
                } else
                    dir = new File(cwd, tag);
                cwd = dir.getAbsolutePath();
                load(dir);
                notifyDataSetChanged();
            };

            file_listener = view -> {
                String tag = (String) view.getTag();
                if (tag == null) return;

                File file = new File(cwd, tag);
                Uri uri = Uri.fromFile(file);
                setResult(RESULT_OK, getIntent().setData(uri));
                finish();
            };
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return ViewType.ENTRY_PARENT;

            File file = entries[position - 1];
            if (file.isDirectory()) return ViewType.ENTRY_DIRECTORY;
            if (file.isFile()) return ViewType.ENTRY_FILE;
            return ViewType.ENTRY_UNKNOWN;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.content_file_selection, parent, false);
            ViewHolder holder = new ViewHolder(view);
            ImageView entry_type = view.findViewById(R.id.entry_type);
            switch (viewType) {
                case ViewType.ENTRY_PARENT: {
                    entry_type.setImageResource(R.drawable.ic_folderup);
                    view.setOnClickListener(dir_listener);
                    break;
                }
                case ViewType.ENTRY_DIRECTORY: {
                    entry_type.setImageResource(R.drawable.ic_folder);
                    view.setOnClickListener(dir_listener);
                    break;
                }
                case ViewType.ENTRY_FILE: {
                    view.setOnClickListener(file_listener);
                    break;
                }
                case ViewType.ENTRY_UNKNOWN: {
                    entry_type.setImageResource(android.R.drawable.ic_delete);
                    break;
                }
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            @ViewType
            int type = holder.getItemViewType();

            if (type == ViewType.ENTRY_PARENT) {
                holder.name.setText(cwd);
                holder.itemView.setTag("..");
                return;
            }

            File file = entries[position - 1];
            String name = file.getName();
            holder.name.setText(name);
            if (type != ViewType.ENTRY_UNKNOWN)
                holder.itemView.setTag(name);
        }

        @Override
        public int getItemCount() {
            return 1 + (entries == null ? 0 : entries.length);
        }

        private void load(File dir) {
            entries = dir.listFiles();
            if (entries == null) return;
            Arrays.sort(entries, (f1, f2) -> {
                if (f1.isDirectory()) {
                    if (f2.isDirectory())
                        return f1.getName().compareTo(f2.getName());
                    return -1;
                }
                if (f1.isFile()) {
                    if (f2.isDirectory()) return 1;
                    if (f2.isFile())
                        return f1.getName().compareTo(f2.getName());
                    return -1;
                }
                // non-existent symbolic link
                if (f2.isDirectory()) return 1;
                if (f2.isFile()) return 1;
                return f1.getName().compareTo(f2.getName());
            });
        }
    }
}
