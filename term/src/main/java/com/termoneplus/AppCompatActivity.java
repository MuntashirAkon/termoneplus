/*
 * Copyright (C) 2019 Roumen Petrov.  All rights reserved.
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

import android.view.Gravity;
import android.widget.Toast;


public class AppCompatActivity extends androidx.appcompat.app.AppCompatActivity {

    protected void restart(int rid) {
        if (rid != 0) {
            Toast toast = Toast.makeText(getApplicationContext(), rid, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        /* Let use function from API level 11
        Intent intent = Intent.makeRestartActivityTask(getComponentName());
        startActivity(intent);
        finish();
        */
        recreate();
    }
}
