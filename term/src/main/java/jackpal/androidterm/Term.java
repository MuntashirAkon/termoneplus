/*
 * Copyright (C) 2007 The Android Open Source Project
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

package jackpal.androidterm;

import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.termoneplus.*;
import com.termoneplus.utils.ConsoleStartupScript;
import com.termoneplus.utils.SimpleClipboardManager;
import com.termoneplus.utils.WakeLock;
import com.termoneplus.utils.WrapOpenURL;
import jackpal.androidterm.compat.PathCollector;
import jackpal.androidterm.compat.PathSettings;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.emulatorview.UpdateCallback;
import jackpal.androidterm.emulatorview.compat.KeycodeConstants;
import jackpal.androidterm.util.SessionList;
import jackpal.androidterm.util.TermSettings;

import java.io.IOException;


/**
 * A terminal emulator activity.
 */
public class Term extends BaseActivity
        implements UpdateCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int REQUEST_CHOOSE_WINDOW = 1;
    /**
     * The name of the ViewFlipper in the resources.
     */
    private static final int VIEW_FLIPPER = R.id.view_flipper;
    private final static int SELECT_TEXT_ID = 0;
    private final static int COPY_ALL_ID = 1;
    private final static int PASTE_ID = 2;
    private final static int SEND_CONTROL_KEY_ID = 3;
    private final static int SEND_FN_KEY_ID = 4;
    /**
     * The ViewFlipper which holds the collection of EmulatorView widgets.
     */
    private TermViewFlipper mViewFlipper;
    private SessionList mTermSessions;
    private TermSettings mSettings;
    private PathSettings path_settings;
    private boolean mAlreadyStarted = false;
    private boolean mStopServiceOnFinish = false;
    private Intent TSIntent;
    private int onResumeSelectWindow = -1;
    private WifiManager.WifiLock mWifiLock;
    private boolean path_collected;
    private TermService mTermService;
    private TermActionBar mActionBar;
    private int mActionBarMode;
    private WindowListAdapter mWinListAdapter;
    private boolean mHaveFullHwKeyboard = false;
    /**
     * Should we use keyboard shortcuts?
     */
    private boolean mUseKeyboardShortcuts;
    /**
     * Intercepts keys before the view/terminal gets it.
     */
    private final View.OnKeyListener mKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return backkeyInterceptor(keyCode, event) || keyboardShortcuts(keyCode, event);
        }

        /**
         * Keyboard shortcuts (tab management, paste)
         */
        private boolean keyboardShortcuts(int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (!mUseKeyboardShortcuts) {
                return false;
            }
            boolean isCtrlPressed = (event.getMetaState() & KeycodeConstants.META_CTRL_ON) != 0;
            boolean isShiftPressed = (event.getMetaState() & KeycodeConstants.META_SHIFT_ON) != 0;

            if (keyCode == KeycodeConstants.KEYCODE_TAB && isCtrlPressed) {
                if (isShiftPressed) {
                    mViewFlipper.showPrevious();
                } else {
                    mViewFlipper.showNext();
                }

                return true;
            } else if (keyCode == KeycodeConstants.KEYCODE_N && isCtrlPressed && isShiftPressed) {
                doCreateNewWindow();

                return true;
            } else if (keyCode == KeycodeConstants.KEYCODE_V && isCtrlPressed && isShiftPressed) {
                doPaste();

                return true;
            } else {
                return false;
            }
        }

        /**
         * Make sure the back button always leaves the application.
         */
        private boolean backkeyInterceptor(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES && mActionBar.isShowing()) {
                /* We need to intercept the key event before the view sees it,
                   otherwise the view will handle it before we get it */
                onKeyUp(keyCode, event);
                return true;
            } else {
                return false;
            }
        }
    };
    private ServiceConnection mTSConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(Application.APP_TAG, "Bound to TermService");
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mTermService = binder.getService();
            populateSessions();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mTermService = null;
        }
    };
    private final Handler mHandler = new Handler();

    protected static TermSession createTermSession(
            Context context,
            TermSettings settings, PathSettings path_settings,
            String extraCommand) throws IOException {

        String initialCommand = !TextUtils.isEmpty(extraCommand) ? extraCommand : "";

        GenericTermSession session = new ShellTermSession(settings, path_settings, initialCommand);
        // XXX We should really be able to fetch this from within TermSession
        session.setProcessExitMessage(context.getString(R.string.process_exit_message));

        return session;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Application.settings.parsePreference(this, sharedPreferences, key);

        if (key.equals(getString(R.string.key_shellrc_preference))) {
            String value = sharedPreferences.getString(key, null);
            ConsoleStartupScript.write(mSettings.getHomePath(), value);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(key);
            editor.apply();
        }

        if (key.equals(getString(R.string.key_home_path_preference))) {
            String value = sharedPreferences.getString(key, null);
            ConsoleStartupScript.rename(mSettings.getHomePath(), value);
            mSettings.setHomePath(value);
        }

        mSettings.readPrefs(this, sharedPreferences);
        path_settings.extractPreferences(sharedPreferences);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Log.v(Application.APP_TAG, "onCreate");

        if (icicle == null)
            onNewIntent(getIntent());

        mSettings = new TermSettings(this);
        path_settings = new PathSettings(this);

        mActionBarMode = mSettings.actionBarMode();

        path_collected = false;
        PathCollector path_collector = new PathCollector(this, path_settings);
        path_collector.setOnPathsReceivedListener(() -> {
            path_collected = true;
            populateSessions();
        });

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        TSIntent = new Intent(this, TermService.class);
        ContextCompat.startForegroundService(this, TSIntent);

        mActionBar = TermActionBar.setTermContentView(this,
                mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES);
        mActionBar.setOnItemSelectedListener(position -> {
            int oldPosition = mViewFlipper.getDisplayedChild();
            if (position == oldPosition) return;

            if (position >= mViewFlipper.getChildCount()) {
                TermSession session = mTermService.getSession(position);
                mViewFlipper.addView(createEmulatorView(session));
            }
            mViewFlipper.setDisplayedChild(position);
            if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES)
                mActionBar.hide();
        });

        mViewFlipper = findViewById(VIEW_FLIPPER);

        Context app = getApplicationContext();

        WakeLock.create(this);

        WifiManager wm = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, Application.APP_TAG);

        mHaveFullHwKeyboard = checkHaveFullHwKeyboard(getResources().getConfiguration());

        updatePrefs();
        requestStoragePermission();
        mAlreadyStarted = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!bindService(TSIntent, mTSConnection, BIND_AUTO_CREATE)) {
            throw new IllegalStateException("Failed to bind to TermService!");
        }
    }

    private synchronized void populateSessions() {
        if (mTermService == null) return;
        if (!path_collected) return;

        if (mTermService.getSessionCount() == 0) {
            try {
                mTermService.addSession(createTermSession());
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),
                        "Failed to start terminal session", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        mTermSessions = mTermService.getSessions();
        mTermSessions.addCallback(this);

        populateViewFlipper();
        populateWindowList();
    }

    private void populateViewFlipper() {
        for (TermSession session : mTermSessions) {
            EmulatorView view = createEmulatorView(session);
            mViewFlipper.addView(view);
        }

        updatePrefs();

        if (onResumeSelectWindow >= 0) {
            onResumeSelectWindow = Math.min(onResumeSelectWindow, mViewFlipper.getChildCount() - 1);
            mViewFlipper.setDisplayedChild(onResumeSelectWindow);
            onResumeSelectWindow = -1;
        }
        mViewFlipper.onResume();
    }

    private void populateWindowList() {
        if (mWinListAdapter == null) {
            mWinListAdapter = new WindowListActionBarAdapter(mTermSessions);

            mActionBar.setAdapter(mWinListAdapter);
        } else {
            mWinListAdapter.setSessions(mTermSessions);
        }
        mViewFlipper.addCallback(mWinListAdapter);

        synchronizeActionBar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        if (mStopServiceOnFinish) {
            stopService(TSIntent);
        }
        mTermService = null;
        mTSConnection = null;
        WakeLock.release();
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private TermSession createTermSession() throws IOException {
        return createTermSession(this, mSettings, path_settings, null);
    }

    private TermView createEmulatorView(TermSession session) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        TermView emulatorView = new TermView(this, session, metrics);

        emulatorView.setExtGestureListener(new EmulatorViewGestureListener(emulatorView));
        emulatorView.setOnKeyListener(mKeyListener);
        emulatorView.setOnToggleSelectingTextListener(null);
        registerForContextMenu(emulatorView);

        return emulatorView;
    }

    private TermSession getCurrentTermSession() {
        if (mTermService == null) return null;

        return mTermService.getSession(mViewFlipper.getDisplayedChild());
    }

    private EmulatorView getCurrentEmulatorView() {
        return (EmulatorView) mViewFlipper.getCurrentView();
    }

    protected void updatePrefs() {
        mUseKeyboardShortcuts = mSettings.getUseKeyboardShortcutsFlag();

        mViewFlipper.updatePrefs(mSettings);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        for (View v : mViewFlipper) {
            ((EmulatorView) v).setDensity(metrics);
            ((TermView) v).updatePrefs(mSettings);
        }

        if (mTermSessions != null) {
            for (TermSession session : mTermSessions) {
                ((GenericTermSession) session).updatePrefs(mSettings);
            }
        }

        {
            Window win = getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            final int FULLSCREEN = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            int desiredFlag = mSettings.showStatusBar() ? 0 : FULLSCREEN;
            if (desiredFlag != (params.flags & FULLSCREEN)) {
                if (mAlreadyStarted) {
                    // Can't switch to/from fullscreen after
                    // starting the activity.
                    restart(R.string.restart_statusbar_change);
                    return;
                } else {
                    win.setFlags(desiredFlag, FULLSCREEN);
                }
            }
            mViewFlipper.setFullScreen((params.flags & FULLSCREEN) != 0);
        }
        if (mActionBarMode != mSettings.actionBarMode()) {
            if (mAlreadyStarted) {
                // Can't switch to new layout after
                // starting the activity.
                restart(R.string.restart_actionbar_change);
                return;
            } else {
                if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
                    mActionBar.hide();
                }
            }
        }

        @TermSettings.Orientation
        int orientation = mSettings.getScreenOrientation();
        int o = 0;
        if (orientation == TermSettings.ORIENTATION_UNSPECIFIED) {
            o = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        } else if (orientation == TermSettings.ORIENTATION_LANDSCAPE) {
            o = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if (orientation == TermSettings.ORIENTATION_PORTRAIT) {
            o = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            /* Shouldn't be happened. */
        }
        setRequestedOrientation(o);
    }

    @Override
    public void onPause() {
        super.onPause();

        /* Explicitly close the input method
           Otherwise, the soft keyboard could cover up whatever activity takes
           our place */
        final IBinder token = mViewFlipper.getWindowToken();
        new Thread() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(token, 0);
            }
        }.start();
    }

    @Override
    protected void onStop() {
        mViewFlipper.onPause();
        if (mTermSessions != null) {
            mTermSessions.removeCallback(this);

            if (mWinListAdapter != null) {
                mTermSessions.removeCallback(mWinListAdapter);
                mTermSessions.removeTitleChangedListener(mWinListAdapter);
                mViewFlipper.removeCallback(mWinListAdapter);
            }
        }

        mViewFlipper.removeAllViews();

        unbindService(mTSConnection);

        super.onStop();
    }

    private boolean checkHaveFullHwKeyboard(Configuration c) {
        return (c.keyboard == Configuration.KEYBOARD_QWERTY) &&
                (c.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mHaveFullHwKeyboard = checkHaveFullHwKeyboard(newConfig);

        EmulatorView v = (EmulatorView) mViewFlipper.getCurrentView();
        if (v != null) {
            v.updateSize(false);
        }

        if (mWinListAdapter != null) {
            // Force Android to redraw the label in the navigation dropdown
            mWinListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_new_window) {
            doCreateNewWindow();
        } else if (id == R.id.menu_close_window) {
            confirmCloseWindow();
        } else if (id == R.id.menu_reset) {
            doResetTerminal();
            Toast toast = Toast.makeText(getApplicationContext(), R.string.reset_toast_notification, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else if (id == R.id.menu_toggle_soft_keyboard) {
            doToggleSoftKeyboard();
        } else if (id == R.id.menu_toggle_wakelock) {
            doToggleWakeLock();
        } else if (id == R.id.menu_toggle_wifilock) {
            doToggleWifiLock();
        } else if (id == R.id.nav_window_list) {
            startActivityForResult(new Intent(this, WindowListActivity.class), REQUEST_CHOOSE_WINDOW);
            return true;
        } else if (id == R.id.nav_preferences) {
            doPreferences();
            return true;
        } else if (id == R.id.nav_special_keys) {
            doDocumentKeys();
            return true;
        } else if (id == R.id.nav_action_help) {
            WrapOpenURL.launch(this, R.string.help_url);
            return true;
        }
        // Hide the action bar if appropriate
        if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
            mActionBar.hide();
        }
        return super.onOptionsItemSelected(item);
    }

    private void doCreateNewWindow() {
        if (mTermService == null) {
            Log.w(Application.APP_TAG, "Couldn't create new window because mTermService == null");
            return;
        }

        try {
            TermSession session = createTermSession();

            mTermService.addSession(session);

            TermView view = createEmulatorView(session);
            view.updatePrefs(mSettings);

            mViewFlipper.addView(view);
            mViewFlipper.setDisplayedChild(mViewFlipper.getChildCount() - 1);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),
                    "Failed to create a session", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmCloseWindow() {
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.confirm_window_close_message);
        final Runnable closeWindow = this::doCloseWindow;
        b.setPositiveButton(android.R.string.ok, (dialog, id) -> {
            dialog.dismiss();
            mHandler.post(closeWindow);
        });
        b.setNegativeButton(android.R.string.cancel, null);
        b.show();
    }

    private void doCloseWindow() {
        if (mTermService == null) return;

        EmulatorView view = getCurrentEmulatorView();
        if (view == null) return;

        view.onPause();
        mViewFlipper.removeView(view);
        TermSession session = view.getTermSession();
        if (session != null) session.finish();

        if (mTermService.getSessionCount() > 0)
            mViewFlipper.showNext();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //noinspection SwitchStatementWithTooFewBranches
        switch (requestCode) {
            case REQUEST_CHOOSE_WINDOW:
                if (resultCode == RESULT_OK && data != null) {
                    int position = data.getIntExtra(Application.ARGUMENT_WINDOW_ID, -2);
                    if (position >= 0) {
                        // Switch windows after session list is in sync, not here
                        onResumeSelectWindow = position;
                    } else if (position == -1) {
                        // NOTE do not create new windows (view) here as launch of a
                        // activity cleans indirectly view flipper - see method onStop.
                        // Create only new session and then on service connection view
                        // flipper and etc. will be updated...
                        //doCreateNewWindow();
                        if (mTermService != null) {
                            try {
                                TermSession session = createTermSession();
                                mTermService.addSession(session);
                                onResumeSelectWindow = mTermService.getSessionCount() - 1;
                            } catch (IOException e) {
                                Toast.makeText(this.getApplicationContext(),
                                        "Failed to create a session", Toast.LENGTH_SHORT).show();
                                onResumeSelectWindow = -1;
                            }
                        } else
                            onResumeSelectWindow = -1;
                    }
                } else {
                    // Close the activity if user closed all sessions
                    // TODO the left path will be invoked when nothing happened, but this Activity was destroyed!
                    if (mTermService == null || mTermService.getSessionCount() == 0) {
                        mStopServiceOnFinish = true;
                        finish();
                    }
                }
                //noinspection UnnecessaryReturnStatement
                return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // Don't repeat action if intent comes from history
            return;
        }

        String action = intent.getAction();
        if (TextUtils.isEmpty(action) ||
                /* not from application */
                !intent.getComponent().getPackageName().equals(Application.ID)) {
            return;
        }

        // huge number simply opens new window
        // TODO: add a way to restrict max number of windows per caller (possibly via reusing BoundSession)
        switch (action) {
            case Application.ACTION_OPEN_NEW_WINDOW:
                onResumeSelectWindow = Integer.MAX_VALUE;
                break;
            case Application.ACTION_SWITCH_WINDOW:
                int target = intent.getIntExtra(Application.ARGUMENT_TARGET_WINDOW, -1);
                if (target >= 0) {
                    onResumeSelectWindow = target;
                }
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem wakeLockItem = menu.findItem(R.id.menu_toggle_wakelock);
        MenuItem wifiLockItem = menu.findItem(R.id.menu_toggle_wifilock);
        if (WakeLock.isHeld()) {
            wakeLockItem.setTitle(R.string.disable_wakelock);
        } else {
            wakeLockItem.setTitle(R.string.enable_wakelock);
        }
        if (mWifiLock.isHeld()) {
            wifiLockItem.setTitle(R.string.disable_wifilock);
        } else {
            wifiLockItem.setTitle(R.string.enable_wifilock);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.edit_text);
        menu.add(0, SELECT_TEXT_ID, 0, R.string.select_text);
        menu.add(0, COPY_ALL_ID, 0, R.string.copy_all);
        menu.add(0, PASTE_ID, 0, R.string.paste);
        menu.add(0, SEND_CONTROL_KEY_ID, 0, R.string.send_control_key);
        menu.add(0, SEND_FN_KEY_ID, 0, R.string.send_fn_key);
        if (!canPaste()) {
            menu.getItem(PASTE_ID).setEnabled(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SELECT_TEXT_ID:
                getCurrentEmulatorView().toggleSelectingText();
                return true;
            case COPY_ALL_ID:
                doCopyAll();
                return true;
            case PASTE_ID:
                doPaste();
                return true;
            case SEND_CONTROL_KEY_ID:
                doSendControlKey();
                return true;
            case SEND_FN_KEY_ID:
                doSendFnKey();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES && mActionBar.isShowing()) {
                    mActionBar.hide();
                    return true;
                }
                switch (mSettings.getBackKeyAction()) {
                    case TermSettings.BACK_KEY_STOPS_SERVICE:
                        mStopServiceOnFinish = true;
                    case TermSettings.BACK_KEY_CLOSES_ACTIVITY:
                        finish();
                        return true;
                    case TermSettings.BACK_KEY_CLOSES_WINDOW:
                        doCloseWindow();
                        return true;
                    default:
                        return false;
                }
            case KeyEvent.KEYCODE_MENU:
                if (!mActionBar.isShowing()) {
                    mActionBar.show();
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // Called when the list of sessions changes
    public void onUpdate() {
        if (mTermService == null) return;

        if (mTermService.getSessionCount() == 0) {
            mStopServiceOnFinish = true;
            finish();
            return;
        }

        SessionList sessions = mTermService.getSessions();
        if (sessions.size() < mViewFlipper.getChildCount()) {
            for (int i = 0; i < mViewFlipper.getChildCount(); ++i) {
                EmulatorView v = (EmulatorView) mViewFlipper.getChildAt(i);
                if (!sessions.contains(v.getTermSession())) {
                    v.onPause();
                    mViewFlipper.removeView(v);
                    --i;
                }
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M /*API Level 23*/) return;

        if (Permissions.permissionExternalStorage(this))
            return;

        if (Permissions.shouldShowExternalStorageRationale(this)) {
            Snackbar.make(
                    mViewFlipper,
                    R.string.message_external_storage_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            view -> Permissions.requestPermissionExternalStorage(
                                    this,
                                    Permissions.REQUEST_EXTERNAL_STORAGE))
                    .show();
        } else
            Permissions.requestPermissionExternalStorage(
                    this,
                    Permissions.REQUEST_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Permissions.REQUEST_EXTERNAL_STORAGE) {
            if (Permissions.isPermissionGranted(grantResults)) {
                Snackbar.make(mViewFlipper,
                        R.string.message_external_storage_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                Snackbar.make(mViewFlipper,
                        R.string.message_external_storage_not_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean canPaste() {
        return canPaste(new SimpleClipboardManager(this));
    }

    private boolean canPaste(SimpleClipboardManager clip) {
        return clip.hasText();
    }

    private void doPreferences() {
        startActivity(new Intent(this, TermPreferencesActivity.class));
    }

    private void doResetTerminal() {
        TermSession session = getCurrentTermSession();
        if (session == null) return;

        session.reset();
    }

    private void doCopyAll() {
        TermSession session = getCurrentTermSession();
        if (session == null) return;

        SimpleClipboardManager clip = new SimpleClipboardManager(this);
        clip.setText(session.getTranscriptText().trim());
    }

    private void doPaste() {
        TermSession session = getCurrentTermSession();
        if (session == null) return;

        SimpleClipboardManager clip = new SimpleClipboardManager(this);
        if (!canPaste(clip)) return;

        CharSequence paste = clip.getText();
        if (TextUtils.isEmpty(paste)) return;

        session.write(paste.toString());
    }

    private void doSendControlKey() {
        getCurrentEmulatorView().sendControlKey();
    }

    private void doSendFnKey() {
        getCurrentEmulatorView().sendFnKey();
    }

    private void doDocumentKeys() {
        Resources resources = getResources();
        String message = formatMessage(mSettings.getControlKeyId(), TermSettings.CONTROL_KEY_ID_NONE,
                resources, R.array.control_keys_short_names, R.string.control_key_dialog_control_text,
                R.string.control_key_dialog_control_disabled_text, "CTRLKEY") + "\n\n" +
                formatMessage(mSettings.getFnKeyId(), TermSettings.FN_KEY_ID_NONE, resources,
                        R.array.fn_keys_short_names, R.string.control_key_dialog_fn_text,
                        R.string.control_key_dialog_fn_disabled_text, "FNKEY");

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.control_key_dialog_title)
                .setMessage(message)
                .show();
    }

    private String formatMessage(int keyId, int disabledKeyId,
                                 Resources r, int arrayId,
                                 int enabledId,
                                 int disabledId, String regex) {
        if (keyId == disabledKeyId) {
            return r.getString(disabledId);
        }
        String[] keyNames = r.getStringArray(arrayId);
        String keyName = keyNames[keyId];
        String template = r.getString(enabledId);
        return template.replaceAll(regex, keyName);
    }

    private void doToggleSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

    }

    private void doToggleWakeLock() {
        WakeLock.toggle(this);
        invalidateOptionsMenu();
    }

    private void doToggleWifiLock() {
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        } else {
            mWifiLock.acquire();
        }
        invalidateOptionsMenu();
    }

    private void doUIToggle(int x, int y, int width, int height) {
        switch (mActionBarMode) {
            case TermSettings.ACTION_BAR_MODE_ALWAYS_VISIBLE:
                if (!mHaveFullHwKeyboard) {
                    doToggleSoftKeyboard();
                }
                break;
            case TermSettings.ACTION_BAR_MODE_HIDES:
                if (mHaveFullHwKeyboard || y < height / 2) {
                    mActionBar.doToggleActionBar();
                    return;
                } else {
                    doToggleSoftKeyboard();
                }
                break;
        }
        getCurrentEmulatorView().requestFocus();
    }

    private void synchronizeActionBar() {
        int position = mViewFlipper.getDisplayedChild();
        mActionBar.setSelection(position);
    }

    private class WindowListActionBarAdapter extends WindowListAdapter implements UpdateCallback {

        public WindowListActionBarAdapter(SessionList sessions) {
            super(Term.this);
            setSessions(sessions);
        }

        @SuppressLint("InflateParams")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.actionbar_windowlist, null);
                holder = new ViewHolder();
                holder.title = convertView.findViewById(R.id.title);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.title.setText(getItemTitle(position));
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }

        public void onUpdate() {
            notifyDataSetChanged();
            synchronizeActionBar();
        }

        private class ViewHolder {
            public TextView title;
        }
    }

    private class EmulatorViewGestureListener extends SimpleOnGestureListener {
        private final EmulatorView view;

        public EmulatorViewGestureListener(EmulatorView view) {
            this.view = view;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // Let the EmulatorView handle taps if mouse tracking is active
            if (view.isMouseTrackingActive()) return false;

            //Check for link at tap location
            String link = view.getURLat(e.getX(), e.getY());
            if (link != null)
                WrapOpenURL.launch(Term.this, link);
            else
                doUIToggle((int) e.getX(), (int) e.getY(), view.getVisibleWidth(), view.getVisibleHeight());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float absVelocityX = Math.abs(velocityX);
            float absVelocityY = Math.abs(velocityY);
            if (absVelocityX > Math.max(1000.0f, 2.0 * absVelocityY)) {
                // Assume user wanted side to side movement
                if (velocityX > 0) {
                    // Left to right swipe -- previous window
                    mViewFlipper.showPrevious();
                } else {
                    // Right to left swipe -- next window
                    mViewFlipper.showNext();
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
