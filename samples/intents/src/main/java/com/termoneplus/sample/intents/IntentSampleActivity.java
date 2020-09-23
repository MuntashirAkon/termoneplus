package com.termoneplus.sample.intents;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class IntentSampleActivity extends AppCompatActivity {

    private static final String ACTION_OPEN_NEW_WINDOW = "com.termoneplus.OPEN_NEW_WINDOW";
    private static final String ACTION_RUN_SCRIPT = "com.termoneplus.RUN_SCRIPT";

    private static final String ARGUMENT_WINDOW_HANDLE = "com.termoneplus.WindowHandle";
    private static final String ARGUMENT_SHELL_COMMAND = "com.termoneplus.Command";

    private static final String PERMISSION_RUN_SCRIPT = BuildConfig.TERM_APPLICATION_ID + ".permission.RUN_SCRIPT";
    private static final int REQUEST_PERMISSION_RUN_SCRIPT = 101;

    private static final int REQUEST_WINDOW_HANDLE = 1;
    private String mHandle;

    private View main_layout;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        main_layout = findViewById(R.id.main_layout);

        addClickListener(R.id.openNewWindow, v -> {
            // Intent for opening a new window without providing script
            Intent intent = new Intent(ACTION_OPEN_NEW_WINDOW)
                    .addCategory(Intent.CATEGORY_DEFAULT);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                errorActivityNotFound(v);
            } catch (Exception ignore) {
                // nop
            }
        });

        final EditText script = findViewById(R.id.script);
        script.setText(getString(R.string.default_script));
        addClickListener(R.id.runScript, v -> {
            /* Intent for opening a new window and running the provided
               script -- you must declare the permission
               com.termoneplus.permission.RUN_SCRIPT in your manifest
               to use */
            String command = script.getText().toString();
            Intent intent = new Intent(ACTION_RUN_SCRIPT)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra(ARGUMENT_SHELL_COMMAND, command);
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                errorPermissionDenial(v);
            } catch (ActivityNotFoundException e) {
                errorActivityNotFound(v);
            } catch (Exception ignore) {
                // nop
            }
        });
        addClickListener(R.id.runScriptSaveWindow, v -> {
            /* Intent for running a script in a previously opened window,
               if it still exists
               This will open another window if it doesn't find a match */
            String command = script.getText().toString();
            Intent intent = new Intent(ACTION_RUN_SCRIPT)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra(ARGUMENT_SHELL_COMMAND, command);
            if (mHandle != null) {
                // Identify the targeted window by its handle
                intent.putExtra(ARGUMENT_WINDOW_HANDLE, mHandle);
            }
            /* The handle for the targeted window -- whether newly opened
               or reused -- is returned to us via onActivityResult()
               You can compare it against an existing saved handle to
               determine whether or not a new window was opened */
            try {
                startActivityForResult(intent, REQUEST_WINDOW_HANDLE);
            } catch (SecurityException e) {
                errorPermissionDenial(v);
            } catch (ActivityNotFoundException e) {
                errorActivityNotFound(v);
            } catch (Exception ignore) {
                // nop
            }
        });
    }

    private void addClickListener(int buttonId, OnClickListener onClickListener) {
        findViewById(buttonId).setOnClickListener(onClickListener);
    }

    private void errorActivityNotFound(View view) {
        Toast toast = Toast.makeText(view.getContext(), R.string.error_activity_not_found, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.START | Gravity.LEFT, 0, 0);
        toast.show();
    }

    private void errorPermissionDenial(View view) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M /*API level 23*/) {
            Toast toast = Toast.makeText(view.getContext(), R.string.error_security_reinstall, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.START | Gravity.LEFT, 0, 0);
            toast.show();
        } else {
            // OS supports runtime permissions
            Snackbar.make(main_layout,
                    R.string.error_security_grant,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            v -> permissionRunScript())
                    .show();
        }
    }

    @RequiresApi(23)
    private void permissionRunScript() {
        // Check if application should show an explanation
        // before to request grant of permission.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                PERMISSION_RUN_SCRIPT)) {
            // Explanation must be shown *asynchronously* -- without to block
            // current thread waiting for the user's response!
            // If the user confirms the explanation, try real request of permission.
            Snackbar.make(main_layout,
                    R.string.run_script_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            v -> requestPermissionRunScript())
                    .show();
        } else {
            // If no explanation needed, application can request the permission.
            requestPermissionRunScript();
        }
    }

    @RequiresApi(23)
    private void requestPermissionRunScript() {
        ActivityCompat.requestPermissions(this,
                new String[]{PERMISSION_RUN_SCRIPT},
                REQUEST_PERMISSION_RUN_SCRIPT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_WINDOW_HANDLE && data != null) {
            mHandle = data.getStringExtra(ARGUMENT_WINDOW_HANDLE);
            ((Button) findViewById(R.id.runScriptSaveWindow)).setText(
                    R.string.run_script_existing_window);
        }
    }
}
