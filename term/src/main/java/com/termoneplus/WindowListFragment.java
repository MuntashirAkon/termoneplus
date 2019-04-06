/*
 * Copyright (C) 2018-2019 Roumen Petrov.  All rights reserved.
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

import android.content.Context;
import android.os.Bundle;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.ListFragment;


public class WindowListFragment extends ListFragment implements AdapterView.OnItemClickListener {
    private OnItemSelectedListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnItemSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnItemSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_windowlist, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        WindowListAdapter adapter = new WindowListAdapter(getActivity());

        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (listener != null) listener.onPositionSelected(position);
    }

    public interface OnItemSelectedListener {
        void onPositionSelected(int position);
    }

    public static class CloseButton extends AppCompatImageView {
        public CloseButton(Context context) {
            super(context);
        }

        public CloseButton(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CloseButton(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        // Avoid child elements to share pressed state with their parent.
        // NOTE parent android:clickable attribute set to false is not same!
        // As result parent could be pressed and Fragment receives onItemClick event.
        public void setPressed(boolean pressed) {
            if (pressed && ((View) getParent()).isPressed()) return;

            super.setPressed(pressed);
        }
    }
}
