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

import jackpal.androidterm.emulatorview.ColorScheme;


public class Settings {
    // foreground and background as ARGB color pair
    /* Note keep synchronized with names in @array.entries_color_preference
    and index in @array.entryvalues_color_preference. */
    public static final ColorScheme[] color_schemes = {
            new ColorScheme(0XFF000000, 0XFFFFFFFF) /*black on white*/,
            new ColorScheme(0XFFFFFFFF, 0XFF000000) /*white on black*/,
            new ColorScheme(0XFFFFFFFF, 0XFF344EBD) /*white on blue*/,
            new ColorScheme(0XFF00FF00, 0XFF000000) /*green on black*/,
            new ColorScheme(0XFFFFB651, 0XFF000000) /*amber on black*/,
            new ColorScheme(0XFFFF0113, 0XFF000000) /*red on black*/,
            new ColorScheme(0XFF33B5E5, 0XFF000000) /*holo-blue on black*/,
            new ColorScheme(0XFF657B83, 0XFFFDF6E3) /*solarized light*/,
            new ColorScheme(0XFF839496, 0XFF002B36) /*solarized dark*/,
            new ColorScheme(0XFFAAAAAA, 0XFF000000) /*linux console*/
    };
}
