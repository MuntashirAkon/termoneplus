/*
 * Copyright (C) 2020 Roumen Petrov.  All rights reserved.
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

package com.termoneplus.compat;

import android.icu.lang.UCharacter;
import android.os.Build;

import androidx.annotation.RequiresApi;


public class CharacterCompat {
    public static int charCount(int cp /*code point*/) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N /*API Level 24*/)
            return Compat1.charCount(cp);
        else
            return Compat24.charCount(cp);
    }

    public static int toChars(int cp, char[] dst, int dstIndex) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N /*API Level 24*/)
            return Compat1.toChars(cp, dst, dstIndex);
        else
            return Compat24.toChars(cp, dst, dstIndex);
    }

    private static class Compat1 {
        private static int charCount(int cp) {
            return Character.charCount(cp);
        }

        private static int toChars(int cp, char[] dst, int dstIndex) {
            return Character.toChars(cp, dst, dstIndex);
        }
    }

    @RequiresApi(24)
    private static class Compat24 {
        private static int charCount(int cp) {
            return UCharacter.charCount(cp);
        }

        private static int toChars(int cp, char[] dst, int dstIndex) {
            return UCharacter.toChars(cp, dst, dstIndex);
        }
    }
}
