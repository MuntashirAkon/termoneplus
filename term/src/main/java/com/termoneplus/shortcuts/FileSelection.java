package com.termoneplus.shortcuts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Comparator;

import jackpal.androidterm.R;

public class FileSelection extends Activity {
    private String STATE_CWD = "CWD";

    private String cwd; // current working directory


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_selection);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
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
            RecyclerView view = (RecyclerView) findViewById(R.id.directory_list);
            view.setAdapter(adapter);
        }

        {
            final EditText path_input = (EditText) findViewById(R.id.path);
            path_input.setOnKeyListener(
                    new EditText.OnKeyListener() {
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
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
                                return (true);
                            }
                            return (false);
                        }
                    }
            );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CWD, cwd);
    }

    @IntDef({
            ViewType.UP_ENTRY,
            ViewType.DIR_ENTRY,
            ViewType.FILE_ENTRY,
            ViewType.PATH_ENTRY,
            ViewType.UNKNOWN_ENTRY
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ViewType {
        int UP_ENTRY = 0;
        int DIR_ENTRY = 1;
        int FILE_ENTRY = 2;
        int PATH_ENTRY = 3;
        int UNKNOWN_ENTRY = 9;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        private File file;

        public ViewHolder(View view) {
            super(view);
            name = (TextView) itemView.findViewById(R.id.name);
        }

        public void setFile(File file) {
            this.file = file;
            String file_name = this.file.getName();
            itemView.setTag(file_name);
            name.setText(file_name);
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

            dir_listener = new View.OnClickListener() {
                public void onClick(View view) {
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
                }
            };

            file_listener = new View.OnClickListener() {
                public void onClick(View view) {
                    String tag = (String) view.getTag();
                    if (tag == null) return;

                    File file = new File(cwd, tag);
                    Uri uri = Uri.fromFile(file);
                    setResult(RESULT_OK, getIntent().setData(uri));
                    finish();
                }
            };
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return ViewType.UP_ENTRY;
            if (position > entries.length) return ViewType.PATH_ENTRY;

            File file = entries[position - 1];
            if (file.isDirectory()) return ViewType.DIR_ENTRY;
            if (file.isFile()) return ViewType.FILE_ENTRY;
            return ViewType.UNKNOWN_ENTRY;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent,
                                             int viewType) {
            View view;
            ViewHolder holder = null;

            switch (viewType) {
                case ViewType.UP_ENTRY: {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.entry_item_up, parent, false);
                    view.setOnClickListener(dir_listener);
                    holder = new ViewHolder(view);
                    break;
                }
                case ViewType.DIR_ENTRY: {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.entry_item_directory, parent, false);
                    view.setOnClickListener(dir_listener);
                    holder = new ViewHolder(view);
                    break;
                }
                case ViewType.FILE_ENTRY: {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.entry_item_file, parent, false);
                    view.setOnClickListener(file_listener);
                    holder = new ViewHolder(view);
                    break;
                }
                case ViewType.UNKNOWN_ENTRY: {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.entry_item_unknown, parent, false);
                    holder = new ViewHolder(view);
                    break;
                }
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case ViewType.UP_ENTRY: {
                    holder.name.setText(cwd);
                    break;
                }
                case ViewType.DIR_ENTRY:
                case ViewType.FILE_ENTRY: {
                    File file = entries[position - 1];
                    holder.setFile(file);
                    break;
                }
                case ViewType.UNKNOWN_ENTRY: {
                    File file = entries[position - 1];
                    holder.name.setText(file.getName());
                    break;
                }
            }
        }

        @Override
        public int getItemCount() {
            return 1 + (entries == null ? 0 : entries.length);
        }

        private void load(File dir) {
            entries = dir.listFiles();
            if (entries == null) return;
            Arrays.sort(entries, new Comparator<File>() {
                public int compare(File f1, File f2) {
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
                }
            });
        }
    }
}
