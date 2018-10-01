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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import jackpal.androidterm.R;
import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.emulatorview.UpdateCallback;
import jackpal.androidterm.util.SessionList;

public class WindowListAdapter extends BaseAdapter implements UpdateCallback {
    protected final LayoutInflater inflater;
    private final Context context;
    private SessionList sessions;

    public WindowListAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setSessions(SessionList sessions) {
        if ((sessions == null) && (this.sessions != null)) {
            this.sessions.removeCallback(this);
            this.sessions.removeTitleChangedListener(this);
        }
        // Set to null to avoid extra notification in onUpdate event
        this.sessions = null;
        if (sessions != null) {
            sessions.addCallback(this);
            sessions.addTitleChangedListener(this);
        }
        this.sessions = sessions;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return sessions == null ? 0 : sessions.size();
    }

    @Override
    public TermSession getItem(int position) {
        return sessions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            // NOTE for Adapters parent will apply default layout parameters unless ...!
            convertView = inflater.inflate(R.layout.content_windowlist, null);
            holder = new ViewHolder();
            holder.title = convertView.findViewById(R.id.title);
            holder.close = convertView.findViewById(R.id.close);
            holder.close.setOnClickListener(
                    view -> {
                        int position1 = (Integer) view.getTag();
                        sessions.remove(position1);
                        notifyDataSetChanged();
                    }
            );

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setText(getItemTitle(position));
        holder.close.setTag(position);
        return convertView;
    }

    public void onUpdate() {
        if (sessions == null) return;
        notifyDataSetChanged();
    }

    protected String getItemTitle(int position) {
        String title = null;
        {
            TermSession item = sessions.get(position);
            if (item != null) title = item.getTitle();
        }
        if (TextUtils.isEmpty(title)) {
            title = context.getString(R.string.window_title, position + 1);
        }

        return title;
    }

    private class ViewHolder {
        public TextView title;
        public WindowListFragment.CloseButton close;
    }
}
