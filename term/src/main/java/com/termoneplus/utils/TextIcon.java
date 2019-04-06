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

package com.termoneplus.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;


public class TextIcon {

    public static Bitmap create(@NonNull String text, int color, int width, int height) {
        String text_lines[] = text.split("\\s*\n\\s*");
        int lines = text_lines.length;
        for (int k = 0; k < lines; ++k)
            text_lines[k] = text_lines[k].trim();

        final float shadow_offset = 12.0f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        {   // initialize paint attributes
            paint.setTextSize(192);
            paint.setShadowLayer(shadow_offset / 4.0f, shadow_offset, shadow_offset, 0xFF000000);
            paint.setColor(color);
            paint.setSubpixelText(true);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        int maxAscent = 0;
        float textH, textW, textS;
        {   // get bounds for each text line
            int maxDescent = 0;
            int minL = 1000000;
            int maxR = 0;
            Rect bounds = new Rect();

            for (String line : text_lines) {
                paint.getTextBounds(line, 0, line.length(), bounds);
                maxAscent = Math.max(maxAscent, -bounds.top);
                maxDescent = Math.max(maxDescent, bounds.bottom);
                minL = Math.min(minL, bounds.left);
                maxR = Math.max(maxR, bounds.right);
            }

            int maxH = maxAscent + maxDescent;
            int maxW = maxR - minL;

            // line space: 10% of text line height
            textH = (1.1f * lines - 0.1f) * maxH;
            textW = maxW;
            textS = (lines > 1) ? 1.1f * maxH : maxH;

            textH += shadow_offset;
        }

        int bitmapH, bitmapW;
        {   // calculate bitmap size taking into account requested aspect
            float aspect = (float) width / height;
            // text padding: 7%, i.e. 1 / ( 1 - 2 * 7% ) = 1 / 0.86
            final float scale = 1.0f / 0.86f;
            float size;
            if ((textW / textH) > aspect) {
                size = scale * textW;
                bitmapH = (int) Math.ceil(size / aspect);
                bitmapW = (int) Math.ceil(size);
            } else {
                size = scale * textH;
                bitmapH = (int) Math.ceil(size);
                bitmapW = (int) Math.ceil(size * aspect);
            }
        }
        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(bitmapH, bitmapW, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return null;
        }
        bitmap.setDensity(Bitmap.DENSITY_NONE);

        final float top = (bitmapH - textH - shadow_offset) / 2.0f;
        final float centerV = bitmapW / 2.0f;
        float baseline = top + maxAscent;
        {
            Canvas canvas = new Canvas(bitmap);
            for (int k = 0; k < lines; ++k, baseline += textS)
                canvas.drawText(text_lines[k], centerV, baseline, paint);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public static Bitmap create(Context context, @NonNull String text, int color) {
        Resources r = context.getResources();
        DisplayMetrics dm = r.getDisplayMetrics();

        // launcher icon size = 32 dp * ( dpi / 160 ) * 1.5
        int x = Math.round(dm.xdpi * 0.3f);
        int y = Math.round(dm.ydpi * 0.3f);
        return create(text, color, x, y);
    }
}
