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

package com.termoneplus.shortcuts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.termoneplus.R;
import com.termoneplus.utils.TextIcon;

import androidx.appcompat.app.AlertDialog;


public class ColorValue {

    private int color;
    // alpha / red / green / blue
    private Data data[] = {new Data(), new Data(), new Data(), new Data()};

    private EditText icon_text;
    private TextView hex_code;

    private SeekBar.OnSeekBarChangeListener seekbar_color = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) return;

            int index = (Integer) seekBar.getTag();
            if (data[index].lock) {
                for (int k = 0; k < data.length; k++) {
                    if (!data[k].lock) continue;
                    data[k].color = progress;
                    if (k != index)
                        data[k].seekbar.setProgress(progress);
                    data[k].seekbar.setBackgroundColor(indexToColor(k));
                }
            } else {
                data[index].color = progress;
                data[index].seekbar.setBackgroundColor(indexToColor(index));
            }

            color = Color.argb(data[0].color, data[1].color, data[2].color, data[3].color);
            icon_text.setTextColor(color);
            hex_code.setText(colorAsHexString());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            onProgressChanged(seekBar, seekBar.getProgress(), true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            onProgressChanged(seekBar, seekBar.getProgress(), true);
        }
    };

    private CompoundButton.OnCheckedChangeListener lock_checked = (button, isChecked) -> {
        try {
            String s = (String) button.getTag();
            int index = Integer.parseInt(s);
            data[index].lock = isChecked;
        } catch (Exception e) {
            // nop
        }
    };


    public ColorValue(Context context, final ImageView imgview, final String result[]) {

        color = (Integer) imgview.getTag();
        data[0].color = Color.alpha(color);
        data[1].color = Color.red(color);
        data[2].color = Color.green(color);
        data[3].color = Color.blue(color);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.activity_color_value, null);

        icon_text = view.findViewById(R.id.icon_text);
        icon_text.setTextColor(color);
        icon_text.setText(result[0]);

        data[0].seekbar = view.findViewById(R.id.alpha);
        data[1].seekbar = view.findViewById(R.id.red);
        data[2].seekbar = view.findViewById(R.id.green);
        data[3].seekbar = view.findViewById(R.id.blue);
        for (int k = 0; k < data.length; k++) {
            SeekBar seekbar = data[k].seekbar;
            seekbar.setTag(k);
            seekbar.setMax(0xFF);
            seekbar.setProgress(data[k].color);
            seekbar.setBackgroundColor(indexToColor(k));
            seekbar.setOnSeekBarChangeListener(seekbar_color);
        }

        CheckBox checkbox;
        checkbox = view.findViewById(R.id.lock_alpha);
        checkbox.setOnCheckedChangeListener(lock_checked);
        checkbox = view.findViewById(R.id.lock_red);
        checkbox.setOnCheckedChangeListener(lock_checked);
        checkbox = view.findViewById(R.id.lock_green);
        checkbox.setOnCheckedChangeListener(lock_checked);
        checkbox = view.findViewById(R.id.lock_blue);
        checkbox.setOnCheckedChangeListener(lock_checked);

        hex_code = view.findViewById(R.id.hex_code);
        hex_code.setText(colorAsHexString());

        new AlertDialog.Builder(context)
                .setView(view)
                .setTitle(R.string.addshortcut_make_text_icon)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (which != AlertDialog.BUTTON_POSITIVE) return;

                    String s = icon_text.getText().toString();
                    if (TextUtils.isEmpty(s)) return;

                    Bitmap image = TextIcon.create(s, color, 96, 96);
                    if (image != null) {
                        result[1] = result[0] = s;
                        imgview.setTag(color);
                        imgview.setImageBitmap(image);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    private int indexToColor(int index) {
        switch (index) {
            case 0:
                return Color.argb(data[0].color, 0, 0, 0);
            case 1:
                return Color.argb(0xFF, data[1].color, 0, 0);
            case 2:
                return Color.argb(0xFF, 0, data[2].color, 0);
            case 3:
                return Color.argb(0xFF, 0, 0, data[3].color);
        }
        return Color.WHITE;
    }

    private String colorAsHexString() {
        return String.format("#%08X", color);
    }


    private static class Data {
        int color;
        boolean lock;
        SeekBar seekbar;

        Data() {
            color = 0xFF;
            lock = false;
        }
    }
}
