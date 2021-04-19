/*
 * Copyright (C) 2019-2020 Roumen Petrov.  All rights reserved.
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

package com.termoneplus.services;

import android.os.Process;
import android.text.TextUtils;

import com.termoneplus.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import jackpal.androidterm.TermService;
import jackpal.androidterm.compat.PathSettings;


public class CommandService implements UnixSocketServer.ConnectionHandler {
    private static final String socket_prefix = BuildConfig.APPLICATION_ID + "-app_info-";

    private final TermService service;
    private UnixSocketServer socket;

    public CommandService(TermService service) {
        this.service = service;
        try {
            socket = new UnixSocketServer(socket_prefix + Process.myUid(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (socket == null) return;
        socket.start();
    }

    public void stop() {
        if (socket == null) return;
        socket.stop();
        socket = null;
    }

    @Override
    public void handle(InputStream basein, OutputStream baseout) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(basein));

        // Note only one command per connection!
        String line = in.readLine();
        if (TextUtils.isEmpty(line)) return;

        PrintStream out = new PrintStream(baseout);
        if ("get aliases".equals(line)) {// force interactive shell
            out.println("alias sh='sh -i'");
            printExternalAliases(out);
        }
        out.flush();
    }

    private void printExternalAliases(PrintStream out) {
        final Pattern pattern = Pattern.compile("libexec-(.*).so");

        for (String entry : PathSettings.buildPATH().split(File.pathSeparator)) {
            File dir = new File(entry);

            File[] cmdlist = null;
            try {
                cmdlist = dir.listFiles(file -> pattern.matcher(file.getName()).matches());
            } catch (Exception ignore) {
            }
            if (cmdlist == null) continue;

            for (File cmd : cmdlist) {
                ProcessBuilder pb = new ProcessBuilder(cmd.getPath(), "aliases");
                try {
                    java.lang.Process p = pb.start();

                    // close process "input stream" to prevent command
                    // to wait for user input.
                    p.getOutputStream().close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while (true) {
                        String line = in.readLine();
                        if (line == null) break;
                        out.println(line);
                    }
                    out.flush();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
