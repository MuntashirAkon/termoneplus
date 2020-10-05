/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2018-2020 Roumen Petrov.  All rights reserved.
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

import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.termoneplus.Application;
import com.termoneplus.Process;

import jackpal.androidterm.compat.PathSettings;
import jackpal.androidterm.util.TermSettings;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * A terminal session, controlling the process attached to the session (usually
 * a shell). It keeps track of process PID and destroys it's process group
 * upon stopping.
 */
public class ShellTermSession extends GenericTermSession {
    private int mProcId;
    private Thread mWatcherThread;

    private String mInitialCommand;

    private static final int PROCESS_EXITED = 1;
    private Handler mMsgHandler = new ProcessHandler(this);


    public ShellTermSession(TermSettings settings, PathSettings path_settings, String initialCommand) throws IOException {
        super(ParcelFileDescriptor.open(new File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE),
                settings, false);

        mInitialCommand = initialCommand;

        mProcId = createShellProcess(settings, path_settings);

        mWatcherThread = new Thread() {
            @Override
            public void run() {
                Log.i(Application.APP_TAG, "waiting for: " + mProcId);
                int result = Process.waitExit(mProcId);
                Log.i(Application.APP_TAG, "Subprocess exited: " + result);
                mMsgHandler.sendMessage(mMsgHandler.obtainMessage(PROCESS_EXITED, result));
            }
        };
        mWatcherThread.setName("Process watcher");
    }

    @Override
    public void initializeEmulator(int columns, int rows) {
        super.initializeEmulator(columns, rows);

        mWatcherThread.start();
        sendInitialCommand(mInitialCommand);
    }

    private void sendInitialCommand(String initialCommand) {
        if (initialCommand.length() > 0) {
            write(initialCommand + '\r');
        }
    }

    private int createShellProcess(TermSettings settings, PathSettings path_settings) throws IOException {
        String shell = settings.getShell();

        ArrayList<String> argList = parse(shell);
        String arg0;
        String[] args;

        try {
            arg0 = argList.get(0);
            File file = new File(arg0);
            if (!file.exists()) {
                Log.e(Application.APP_TAG, "Shell " + arg0 + " not found!");
                throw new FileNotFoundException(arg0);
            } else if (!file.canExecute()) {
                Log.e(Application.APP_TAG, "Shell " + arg0 + " not executable!");
                throw new FileNotFoundException(arg0);
            }
            args = argList.toArray(new String[0]);
        } catch (Exception e) {
            argList = parse(settings.getFailsafeShell());
            arg0 = argList.get(0);
            args = argList.toArray(new String[0]);
        }

        Map<String, String> map = new HashMap<>(System.getenv());
        map.put("TERM", settings.getTermType());
        map.put("PATH", Application.xbindir.getPath() + File.pathSeparator + path_settings.buildPATH());
        map.put("HOME", settings.getHomePath());
        map.put("TMPDIR", Application.getTmpPath());
        map.put("ENV", Application.getScriptFilePath());

        String[] env = new String[map.size()];
        int k = 0;
        for (Map.Entry<String, String> entry : map.entrySet())
            env[k++] = entry.getKey() + "=" + entry.getValue();

        return Process.createSubprocess(mTermFd, arg0, args, env);
    }

    private ArrayList<String> parse(String cmd) {
        final int PLAIN = 0;
        final int WHITESPACE = 1;
        final int INQUOTE = 2;
        int state = WHITESPACE;
        ArrayList<String> result =  new ArrayList<>();
        int cmdLen = cmd.length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cmdLen; i++) {
            char c = cmd.charAt(i);
            if (state == PLAIN) {
                if (Character.isWhitespace(c)) {
                    result.add(builder.toString());
                    builder.delete(0,builder.length());
                    state = WHITESPACE;
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    builder.append(c);
                }
            } else if (state == WHITESPACE) {
                if (Character.isWhitespace(c)) {
                    // do nothing
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    state = PLAIN;
                    builder.append(c);
                }
            } else if (state == INQUOTE) {
                if (c == '\\') {
                    if (i + 1 < cmdLen) {
                        i += 1;
                        builder.append(cmd.charAt(i));
                    }
                } else if (c == '"') {
                    state = PLAIN;
                } else {
                    builder.append(c);
                }
            }
        }
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }

    private void onProcessExit(int result) {
        onProcessExit();
    }

    @Override
    public void finish() {
        Process.finishChilds(mProcId);
        super.finish();
    }

    private static class ProcessHandler extends Handler {
        private final WeakReference<ShellTermSession> reference;

        ProcessHandler(ShellTermSession session) {
            reference = new WeakReference<>(session);
        }

        @Override
        public void handleMessage(Message msg) {
            ShellTermSession session = reference.get();
            if (session == null) return;
            if (!session.isRunning()) return;

            if (msg.what == PROCESS_EXITED)
                session.onProcessExit((Integer) msg.obj);
        }
    }
}
