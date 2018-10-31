/*
 * Copyright (C) 2007 The Android Open Source Project
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

package jackpal.androidterm.compat;


/* NOTE: refactored path settings from TermSettings.java
 * TODO: pending removal as functionality does not support multiple entries.
 */
public class PathSettings {
    private String mPrependPath = null;
    private String mAppendPath = null;

    public String getPrependPath() {
        return mPrependPath;
    }

    public void setPrependPath(String prependPath) {
        mPrependPath = prependPath;
    }

    public String getAppendPath() {
        return mAppendPath;
    }

    public void setAppendPath(String appendPath) {
        mAppendPath = appendPath;
    }
}
