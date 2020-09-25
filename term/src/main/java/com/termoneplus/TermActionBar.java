/*
 * Copyright (C) 2017-2020 Roumen Petrov.  All rights reserved.
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
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;


/**
 * An action bar for terminal emulator activity.
 */

public class TermActionBar {
    private final DrawerLayout drawer;
    private final NavigationView nav_view;
    private final Toolbar toolbar;
    private final Spinner spinner;

    private TermActionBar(AppCompatActivity context, boolean floating) {
        toolbar = context.findViewById(R.id.toolbar);
        context.setSupportActionBar(toolbar);

        drawer = context.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                context, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerOpened(View drawerView) {
                hideSoftInput(drawerView);
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        nav_view = context.findViewById(R.id.nav_view);
        NavigationBackground.presetColors(context, nav_view);

        ActionBar appbar = context.getSupportActionBar();
        if (appbar != null) {
            appbar.setDisplayShowTitleEnabled(false);
            appbar.setDisplayShowHomeEnabled(false);
        }

        spinner = context.findViewById(R.id.spinner);

        if (floating)
            hide();
    }

    public static TermActionBar setTermContentView(AppCompatActivity context, boolean floating) {
        if (floating)
            context.setContentView(R.layout.drawer_term_floatbar);
        else
            context.setContentView(R.layout.drawer_term);

        return new TermActionBar(context, floating);
    }

    public void setAdapter(WindowListAdapter adapter) {
        spinner.setAdapter(adapter);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        AdapterView.OnItemSelectedListener wrapper = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
        spinner.setOnItemSelectedListener(wrapper);
    }

    public void setOnNavigationItemSelectedListener(
            NavigationView.OnNavigationItemSelectedListener listener
    ) {
        nav_view.setNavigationItemSelectedListener(item -> {
            boolean result = listener.onNavigationItemSelected(item);
            drawer.closeDrawer(GravityCompat.START);
            return result;
        });
    }

    public void setSelection(int position) {
        spinner.setSelection(position);
    }

    public boolean isShowing() {
        return toolbar.getVisibility() == View.VISIBLE;
    }

    public void hide() {
        toolbar.setVisibility(View.GONE);
    }

    public void show() {
        toolbar.setVisibility(View.VISIBLE);
    }

    public void doToggleActionBar() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

    public void lockDrawer(boolean flag) {
        if (flag)
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        else
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private void hideSoftInput(final View view) {
        new Thread() {
            @Override
            public void run() {
                Context context = view.getContext();

                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;

                android.os.IBinder token = view.getWindowToken();
                imm.hideSoftInputFromWindow(token, 0);
            }
        }.start();
    }

    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }

    private static class NavigationBackground {
        private static void presetColors(AppCompatActivity context, NavigationView view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP /* API Level 21*/) {
                // managed as attribute in drawable header_background
                return;
            }

            @ColorInt final int[] colors = {
                    // see header_background
                    0x78909C /* Blue Gray 50, 400 */,
                    0x607D8B /* Blue Gray 50, 500 */,
                    0x455A64 /* Blue Gray 50, 700 */
            };
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            if (theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true))
                colors[0] = typedValue.data;
            if (theme.resolveAttribute(R.attr.colorPrimary, typedValue, true))
                colors[1] = typedValue.data;
            if (theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true))
                colors[2] = typedValue.data;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN /* API Level 16*/)
                Compat16.setColors(view, colors);
            else
                Compat1.setColors(view, colors);
        }

        @RequiresApi(16)
        private static class Compat16 {
            private static void setColors(NavigationView view, int[] colors) {
                try {
                    View header = view.getHeaderView(0);
                    GradientDrawable drawable = (GradientDrawable) header.getBackground();
                    drawable.setColors(colors);
                } catch (Exception ignore) {
                }
            }
        }

        private static class Compat1 {
            // note suppression is not redundant - for setBackgroundDrawable
            @SuppressWarnings("deprecation")
            private static void setColors(NavigationView view, int[] colors) {
                try {
                    View header = view.getHeaderView(0);
                    GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
                    header.setBackgroundDrawable(drawable);
                } catch (Exception ignore) {
                }
            }
        }
    }
}
